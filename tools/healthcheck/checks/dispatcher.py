import logging
import os
import re
from concurrent.futures import ThreadPoolExecutor
from typing import Dict
from collections import OrderedDict

from prometheus_client import Gauge

from checks.checker import IChecker
from checks.status import Status, aggregate_statuses
from http_client import http_session

from config import DISPATCHER_CONFIG_FILE_PATH

# DISPATCHER_CONFIG_FILE_PATH must point to a file where each non-empty,
# non-comment line has the format: <dispatcher_name>;<dispatcher_health_url>
# The path may be absolute or relative to the healthcheck root directory.
# Example:
# dispatcher-15-15_v1.5;http://localhost:8080/actuator/health
# dispatcher-15-nexsis_v1.9;http://localhost:8081/actuator/health

PERIMETER_15_15 = "15-15"
PERIMETER_15_18 = "15-18"
PERIMETERS = (PERIMETER_15_15, PERIMETER_15_18)
# Perimeter of the dispatcher (from its name) -> perimeter of the platform status.
# A dispatcher with another perimeter (15-smur, 15-gps, 15-sas...) is in no perimeter.
PERIMETER_BY_DISPATCHER_PERIMETER = {
    "15-15": PERIMETER_15_15,
    "15-nexsis": PERIMETER_15_18,
}
DISPATCHER_NAME_PATTERN = re.compile(r"^dispatcher-(?P<perimeter>.+)_v[^_]*$")
DISPATCHERS_AGGREGATE_NAME = "dispatchers"
# Known actuator statuses are returned unchanged; any unrecognized status falls back to DOWN.
DISPATCHER_ACCEPTED_STATUSES = (Status.UP.value, Status.DEGRADED.value)


def perimeter_from_name(name: str) -> str | None:
    """Perimeter of the platform status of a dispatcher named
    dispatcher-<perimeter>_v<version>, or None if its perimeter is not monitored."""
    match = DISPATCHER_NAME_PATTERN.match(name)
    if match is None:
        return None
    return PERIMETER_BY_DISPATCHER_PERIMETER.get(match.group("perimeter"))


class DispatchersHealthcheck(IChecker):
    dispatcher_status_metric = Gauge(
        "dispatcher_status",
        "Statut des dispatchers (1=joignable: UP ou DEGRADED, 0=DOWN)",
        ["dispatcher"],
    )

    def __init__(self):
        self.dispatcher_instances: Dict[str, str] = {}
        self.dispatcher_perimeters: Dict[str, str | None] = {}
        self._load_config()
        self._init_metrics()

    def _build_config_file_path(self) -> str:
        if os.path.isabs(DISPATCHER_CONFIG_FILE_PATH):
            return DISPATCHER_CONFIG_FILE_PATH

        healthcheck_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
        return os.path.join(healthcheck_dir, DISPATCHER_CONFIG_FILE_PATH)

    def _load_config(self):
        path = self._build_config_file_path()

        if not os.path.exists(path):
            raise FileNotFoundError(f"Dispatcher config file not found: {path}")

        with open(path, encoding="utf-8") as f:
            for line in f:
                parsed = self._parse_line(line)
                if parsed is None:
                    continue
                name, url = parsed
                self.dispatcher_instances[name] = url
                self.dispatcher_perimeters[name] = perimeter_from_name(name)
                if self.dispatcher_perimeters[name] is None:
                    logging.debug("Dispatcher %s is in no monitored perimeter", name)

        if not self.dispatcher_instances:
            raise RuntimeError(f"No dispatchers loaded from {path}")

        logging.info("Loaded %d dispatchers", len(self.dispatcher_instances))

    def _parse_line(self, line: str) -> tuple[str, str] | None:
        line = line.strip()

        if not line or line.startswith("#"):
            return

        if ";" not in line:
            logging.warning("Invalid config line (ignored): %s", line)
            return

        name, url = line.split(";", 1)
        name = name.strip()
        url = url.strip()

        if not name or not url:
            logging.warning("Incomplete config line (ignored): %s", line)
            return

        return name, url

    def _init_metrics(self):
        for dispatcher in self.dispatcher_instances:
            self.dispatcher_status_metric.labels(dispatcher=dispatcher).set(0)

    def perform_checks(self) -> Dict[str, dict]:
        with ThreadPoolExecutor() as executor:
            futures = {
                name: executor.submit(self.single_dispatcher_healthcheck, name, url)
                for name, url in self.dispatcher_instances.items()
            }
            result = {name: future.result() for name, future in futures.items()}

        result[DISPATCHERS_AGGREGATE_NAME] = {
            "status": aggregate_statuses(
                instance["status"] for instance in result.values()
            )
        }
        return result

    def check_failure_fallback(self) -> Dict[str, dict]:
        result = {}

        for name in self.dispatcher_instances:
            result[name] = {"status": Status.DOWN.value}
            self.dispatcher_status_metric.labels(dispatcher=name).set(0)

        result[DISPATCHERS_AGGREGATE_NAME] = {"status": Status.DOWN.value}
        return result

    def single_dispatcher_healthcheck(self, name: str, url: str) -> dict:
        logging.info("Checking health of dispatcher: %s", name)

        try:
            response = http_session.get(url)
            response.raise_for_status()

            data = response.json()
            status = data.get("status", Status.UNKNOWN.value)
            if status not in DISPATCHER_ACCEPTED_STATUSES:
                status = Status.DOWN.value

            is_reachable = status in DISPATCHER_ACCEPTED_STATUSES
            self.dispatcher_status_metric.labels(dispatcher=name).set(
                1 if is_reachable else 0
            )

            return OrderedDict(
                [
                    ("status", status),
                    ("components", data.get("components", {})),
                ]
            )

        except Exception:
            logging.error(
                "An error occured during healthcheck for %s", name, exc_info=True
            )

        self.dispatcher_status_metric.labels(dispatcher=name).set(0)
        return {"status": Status.DOWN.value}
