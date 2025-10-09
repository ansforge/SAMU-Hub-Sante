import requests
import logging
from prometheus_client import Gauge

from checks.status import Status
from config import (
    HTTP_TIMEOUT,
    RABBITMQ_HEALTH_URL,
    RABBITMQ_MONITORING_USERNAME,
    RABBITMQ_MONITORING_PASSWORD,
    RABBITMQ_CA_CERT_PATH,
)

rabbitmq_status_metric = Gauge("rabbitmq_status", "Statut de RabbitMQ (1=UP, 0=DOWN)")


def rabbitmq_healthcheck():
    try:
        response = requests.get(
            RABBITMQ_HEALTH_URL,
            auth=(RABBITMQ_MONITORING_USERNAME, RABBITMQ_MONITORING_PASSWORD),
            verify=RABBITMQ_CA_CERT_PATH,
            timeout=HTTP_TIMEOUT,
        )
        response.raise_for_status()
        status = response.json().get("status", Status.UNKNOWN.value)
        rabbitmq_status_metric.set(1 if status == Status.OK.value else 0)
        return (
            {"status": Status.UP.value}
            if status == Status.OK.value
            else {"status": Status.DOWN.value}
        )
    except requests.RequestException as e:
        logging.error(
            "Error occurred on RabbitMQ server's healthcheck: ", exc_info=True
        )
        rabbitmq_status_metric.set(0)
        return {"status": Status.DOWN.value}
