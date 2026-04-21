import logging
import os
from concurrent.futures import ThreadPoolExecutor
from typing import Dict
from collections import OrderedDict

from prometheus_client import Gauge

from checks.checker import IChecker
from checks.status import Status
from http_client import http_session

from config import DISPATCHER_CONFIG_FILE_PATH
# DISPATCHER_CONFIG_FILE_PATH must point to a file where each non-empty,
# non-comment line has the format: <dispatcher_name>;<dispatcher_health_url>
# The path may be absolute or relative to the healthcheck root directory.
# Example:
# dispatcher1;http://localhost:8080/actuator/health
# dispatcher2;http://localhost:8081/actuator/health


class DispatchersHealthcheck(IChecker):
    dispatcher_status_metric = Gauge(
        "dispatcher_status",
        "Statut des dispatchers (1=UP, 0=DOWN)",
        ["dispatcher"],
    )

    def __init__(self):
        self.dispatcher_instances: Dict[str, str] = {}
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
            return {name: future.result() for name, future in futures.items()}

    def check_failure_fallback(self) -> Dict[str, dict]:
        result = {}

        for name in self.dispatcher_instances:
            result[name] = {"status": Status.DOWN.value}
            self.dispatcher_status_metric.labels(dispatcher=name).set(0)

        return result

    def single_dispatcher_healthcheck(self, name: str, url: str) -> dict:
        logging.info("Checking health of dispatcher: %s", name)

        try:
            response = http_session.get(url)
            response.raise_for_status()

            data = response.json()
            status = data.get("status", Status.UNKNOWN.value)

            is_up = status == Status.UP.value
            self.dispatcher_status_metric.labels(dispatcher=name).set(1 if is_up else 0)

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
