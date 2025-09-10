import os
import sys
import requests
import json
import logging
from flask import Flask, jsonify, Response, request
from collections import OrderedDict
from prometheus_flask_exporter import PrometheusMetrics
from prometheus_client import Gauge
from enum import Enum

app = Flask(__name__)
metrics = PrometheusMetrics(app)

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class Status(Enum):
    UP = "UP"
    DOWN = "DOWN"
    OK = "ok"
    UNKNOWN = "UNKNOWN"

# Required environment variables
REQUIRED_ENV_VARS = [
    "RABBITMQ_URL",
    "RABBITMQ_MONITORING_USERNAME",
    "RABBITMQ_MONITORING_PASSWORD",
    "DISPATCHER_INSTANCES"
]

# Check all required environment variables
missing_vars = [var for var in REQUIRED_ENV_VARS if not os.getenv(var)]
if missing_vars:
    sys.exit(f"Error: The following environment variables are not set: {', '.join(missing_vars)}")

RABBITMQ_URL = os.getenv("RABBITMQ_URL")
RABBITMQ_MONITORING_USERNAME = os.getenv("RABBITMQ_MONITORING_USERNAME")
RABBITMQ_MONITORING_PASSWORD = os.getenv("RABBITMQ_MONITORING_PASSWORD")
RABBITMQ_CA_CERT_PATH = '/etc/ssl/certs/hubsante-rabbitmq-ca.crt'

DISPATCHER_INSTANCES_ENV_VAR = os.getenv("DISPATCHER_INSTANCES")
DISPATCHER_INSTANCES = DISPATCHER_INSTANCES_ENV_VAR.split(",") if DISPATCHER_INSTANCES_ENV_VAR else []

HTTP_TIMEOUT = int(os.getenv("HTTP_TIMEOUT", 5))  # Timeout in seconds, configurable via environment variable

RABBITMQ_HEALTH_URL = f"{RABBITMQ_URL}/rabbitmq/api/health/checks/alarms"
CONVERTER_HEALTH_URL = "http://converter.app.svc.cluster.local:8080/health"
METRICS_ENDPOINT = "/metrics"
HEALTH_ENDPOINT = "/health"
DEFAULT_FLASK_HOST = "0.0.0.0"
DEFAULT_FLASK_PORT = 8080

# Definition of Prometheus metrics
rabbitmq_status_metric = Gauge('rabbitmq_status', 'Statut de RabbitMQ (1=UP, 0=DOWN)')
dispatcher_status_metric = Gauge('dispatcher_status', 'Statut des dispatchers (1=UP, 0=DOWN)', ['dispatcher'])
converter_status_metric = Gauge('converter_status', 'Statut du converter (1=UP, 0=DOWN)')

# Initialize the metric for each dispatcher to DOWN by default
for dispatcher in DISPATCHER_INSTANCES:
    dispatcher_status_metric.labels(dispatcher=dispatcher).set(0)

def rabbitmq_healthcheck():
    try:
        response = requests.get(
            RABBITMQ_HEALTH_URL,
            auth=(RABBITMQ_MONITORING_USERNAME, RABBITMQ_MONITORING_PASSWORD),
            verify=RABBITMQ_CA_CERT_PATH,
            timeout=HTTP_TIMEOUT
        )
        response.raise_for_status()
        status = response.json().get("status", Status.UNKNOWN.value)
        rabbitmq_status_metric.set(1 if status == Status.OK.value else 0)
        return {"status": Status.UP.value} if status == Status.OK.value else {"status": Status.DOWN.value}
    except requests.RequestException as e:
        logger.error("Error occurred on RabbitMQ server's healthcheck: ", exc_info=True)
        rabbitmq_status_metric.set(0)
        return {"status": Status.DOWN.value}

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
        logger.error("Error occurred on dispatcher %s healthcheck: ", app_name, exc_info=True)
        dispatcher_status_metric.labels(dispatcher=app_name).set(0)
        return {"status": Status.DOWN.value}

def converter_healthcheck():
    try:
        response = requests.get(CONVERTER_HEALTH_URL, timeout=HTTP_TIMEOUT)
        response.raise_for_status()
        data = response.json()
        status = data.get("status", Status.UNKNOWN.value)
        converter_status_metric.set(1 if status == Status.UP.value else 0)
        return {"status": Status.UP.value} if status == Status.UP.value else {"status": Status.DOWN.value}

    except requests.RequestException as e:
        logger.error("Error occurred on converter healthcheck: ", exc_info=True)
        converter_status_metric.set(0)
        return {"status": Status.DOWN.value}

@app.before_request
def update_metrics_before_scrapping():
    if request.path == METRICS_ENDPOINT:
        rabbitmq_healthcheck()
        for dispatcher_instance in DISPATCHER_INSTANCES:
            dispatcher_healthcheck(dispatcher_instance)
        converter_healthcheck()

@app.route(HEALTH_ENDPOINT, methods=['GET'])
def health():
    global_status = Status.UP.value
    components = OrderedDict()

    # Fetch RabbitMQ health
    rabbitmq_health = rabbitmq_healthcheck()
    components["rabbitmq_server"] = rabbitmq_health
    if rabbitmq_health["status"] == Status.DOWN.value:
        global_status = Status.DOWN.value

    # Fetch health from Spring apps
    logger.info(f"Checking health of dispatcher instances: {DISPATCHER_INSTANCES}")
    for dispatcher_instance in DISPATCHER_INSTANCES:
        spring_health = dispatcher_healthcheck(dispatcher_instance)
        components[dispatcher_instance] = spring_health
        if spring_health["status"] == Status.DOWN.value:
            global_status = Status.DOWN.value

    converter_health = converter_healthcheck()
    components["converter"] = converter_health
    if converter_health["status"] == Status.DOWN.value:
        global_status = Status.DOWN.value

    # Aggregate and return the result
    result = OrderedDict([
        ("status", global_status),
        ("components", components)
    ])
    return Response(
        response=json.dumps(remove_error_keys(result)),
        mimetype="application/json"
    )

def remove_error_keys(d, component_name='root'):
    if isinstance(d, dict):
        # Check if "error" key exists and log it
        if "error" in d:
            logging.error(f"Error encountered in component '{component_name}': {d['error']}")
            del d["error"]

        # Recursively remove "error" from nested dictionaries
        for key, value in d.items():
            remove_error_keys(value, component_name=f"{component_name}.{key}")

    # If d is a list, iterate through each element
    elif isinstance(d, list):
        for item in d:
            remove_error_keys(item, component_name=component_name)

    return d

if __name__ == "__main__":
    app.run(host=DEFAULT_FLASK_HOST, port=DEFAULT_FLASK_PORT)
