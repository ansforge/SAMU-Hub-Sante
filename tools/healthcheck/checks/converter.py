from prometheus_client import Gauge

from checks.checker import IChecker
from checks.status import Status
from http_client import http_session

from config import CONVERTER_HEALTH_URL


class ConverterHealthcheck(IChecker):
    converter_status_metric = Gauge(
        "converter_status", "Statut du converter (1=UP, 0=DOWN)"
    )

    def perform_checks(self):
        response = http_session.get(CONVERTER_HEALTH_URL)
        response.raise_for_status()
        data = response.json()
        status = data.get("status", Status.UNKNOWN.value)
        self.converter_status_metric.set(1 if status == Status.UP.value else 0)
        return {
            "converter": (
                {"status": Status.UP.value}
                if status == Status.UP.value
                else {"status": Status.DOWN.value}
            )
        }

    def check_failure_fallback(self):
        self.converter_status_metric.set(0)
        return {"converter": {"status": Status.DOWN.value}}
