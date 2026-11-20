from prometheus_client import Gauge

from checks.checker import IChecker
from checks.status import Status
from http_client import http_session

from config import CONVERTER_HEALTH_URL

# Known converter statuses are returned unchanged; any unrecognized status falls back to DOWN.
# DEGRADED means the converter is reachable but cannot access its database.
CONVERTER_ACCEPTED_STATUSES = (Status.UP.value, Status.DEGRADED.value)


class ConverterHealthcheck(IChecker):
    converter_status_metric = Gauge(
        "converter_status", "Statut du converter (1=joignable: UP ou DEGRADED, 0=DOWN)"
    )

    def perform_checks(self):
        response = http_session.get(CONVERTER_HEALTH_URL)
        response.raise_for_status()
        data = response.json()
        status = data.get("status", Status.UNKNOWN.value)
        if status not in CONVERTER_ACCEPTED_STATUSES:
            status = Status.DOWN.value
        self.converter_status_metric.set(0 if status == Status.DOWN.value else 1)
        return {"converter": {"status": status}}

    def check_failure_fallback(self):
        self.converter_status_metric.set(0)
        return {"converter": {"status": Status.DOWN.value}}
