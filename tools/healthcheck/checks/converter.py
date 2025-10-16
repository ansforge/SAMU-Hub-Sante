import requests
import logging
from prometheus_client import Gauge

from checks.checker import IChecker
from checks.status import Status
from config import HTTP_TIMEOUT

CONVERTER_HEALTH_URL = "http://converter.app.svc.cluster.local:8080/health"


class ConverterHealthcheck(IChecker):
    converter_status_metric = Gauge(
        "converter_status", "Statut du converter (1=UP, 0=DOWN)"
    )

    def perform_checks(self):
        try:
            response = requests.get(CONVERTER_HEALTH_URL, timeout=HTTP_TIMEOUT)
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

        except requests.RequestException:
            logging.error("Error occurred on converter healthcheck: ", exc_info=True)
            self.converter_status_metric.set(0)
            return {"converter": {"status": Status.DOWN.value}}
