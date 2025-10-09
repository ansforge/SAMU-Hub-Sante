import requests
import logging
from prometheus_client import Gauge
from collections import OrderedDict

from checks.status import Status
from config import HTTP_TIMEOUT, DISPATCHER_INSTANCES_ENV_VAR

DISPATCHER_INSTANCES = DISPATCHER_INSTANCES_ENV_VAR.split(",") if DISPATCHER_INSTANCES_ENV_VAR else []

dispatcher_status_metric = Gauge('dispatcher_status', 'Statut des dispatchers (1=UP, 0=DOWN)', ['dispatcher'])

# Initialize the metric for each dispatcher to DOWN by default
for dispatcher in DISPATCHER_INSTANCES:
    dispatcher_status_metric.labels(dispatcher=dispatcher).set(0)

def dispatcher_healthcheck(app_name):
    try:
        dispatcher_health_url = f"http://{app_name}.app.svc.cluster.local:8080/actuator/health"
        response = requests.get(dispatcher_health_url, timeout=HTTP_TIMEOUT)
        response.raise_for_status()
        data = response.json()
        status = data.get("status", Status.UNKNOWN.value)
        dispatcher_status_metric.labels(dispatcher=app_name).set(1 if status == Status.UP.value else 0)
        return OrderedDict([
            ("status", status),
            ("components", data.get("components", {}))
        ])
    except requests.RequestException as e:
        logging.error("Error occurred on dispatcher %s healthcheck: ", app_name, exc_info=True)
        dispatcher_status_metric.labels(dispatcher=app_name).set(0)
        return {"status": Status.DOWN.value}