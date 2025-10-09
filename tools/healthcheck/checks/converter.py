import requests
import logging
from prometheus_client import Gauge

from checks.status import Status
from config import HTTP_TIMEOUT

CONVERTER_HEALTH_URL = "http://converter.app.svc.cluster.local:8080/health"

converter_status_metric = Gauge('converter_status', 'Statut du converter (1=UP, 0=DOWN)')

def converter_healthcheck():
    try:
        response = requests.get(CONVERTER_HEALTH_URL, timeout=HTTP_TIMEOUT)
        response.raise_for_status()
        data = response.json()
        status = data.get("status", Status.UNKNOWN.value)
        converter_status_metric.set(1 if status == Status.UP.value else 0)
        return {"status": Status.UP.value} if status == Status.UP.value else {"status": Status.DOWN.value}

    except requests.RequestException as e:
        logging.error("Error occurred on converter healthcheck: ", exc_info=True)
        converter_status_metric.set(0)
        return {"status": Status.DOWN.value}