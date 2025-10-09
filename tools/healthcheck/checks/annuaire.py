import requests
import logging
from prometheus_client import Gauge

from checks.status import Status
from config import HTTP_TIMEOUT

ANNUAIRE_HEALTH_URL = "http://annuaire-service.annuaire.svc.cluster.local:8080/annuaire/health"

annuaire_status_metric = Gauge('annuaire_status', 'Statut de l\'annuaire (1=UP, 0=DOWN)')

def annuaire_healthcheck():
    try:
        response = requests.get(ANNUAIRE_HEALTH_URL, timeout=HTTP_TIMEOUT)
        response.raise_for_status()
        data = response.json()
        status = data.get("status", Status.UNKNOWN.value)
        annuaire_status_metric.set(1 if status == Status.UP.value else 0)
        return {"status": Status.UP.value} if status == Status.UP.value else {"status": Status.DOWN.value}

    except requests.RequestException as e:
        logging.error("Error occurred on annuaire healthcheck: ", exc_info=True)
        annuaire_status_metric.set(0)
        return {"status": Status.DOWN.value}