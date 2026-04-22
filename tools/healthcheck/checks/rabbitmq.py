from prometheus_client import Gauge

from checks.checker import IChecker
from checks.status import Status
from http_client import http_session
from config import (
    RABBITMQ_HEALTH_URL,
    RABBITMQ_MONITORING_USERNAME,
    RABBITMQ_MONITORING_PASSWORD,
    RABBITMQ_CA_CERT_PATH,
)


class RabbitMQHealthcheck(IChecker):
    rabbitmq_status_metric = Gauge(
        "rabbitmq_status", "Statut de RabbitMQ (1=UP, 0=DOWN)"
    )

    def perform_checks(self):
        response = http_session.get(
            RABBITMQ_HEALTH_URL,
            auth=(RABBITMQ_MONITORING_USERNAME, RABBITMQ_MONITORING_PASSWORD),
            verify=RABBITMQ_CA_CERT_PATH,
        )
        response.raise_for_status()
        status = response.json().get("status", Status.UNKNOWN.value)
        self.rabbitmq_status_metric.set(1 if status == Status.OK.value else 0)
        return {
            "rabbitmq_server": (
                {"status": Status.UP.value}
                if status == Status.OK.value
                else {"status": Status.DOWN.value}
            )
        }

    def check_failure_fallback(self):
        self.rabbitmq_status_metric.set(0)
        return {"rabbitmq_server": {"status": Status.DOWN.value}}
