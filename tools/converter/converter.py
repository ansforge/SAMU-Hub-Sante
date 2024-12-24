import os
import sys
import requests
import json
import logging
from flask import Flask, Response
from collections import OrderedDict

app = Flask(__name__)

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

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

def rabbitmq_healthcheck():
    try:
        response = requests.get(
            f"{RABBITMQ_URL}/rabbitmq/api/health/checks/alarms",
            auth=(RABBITMQ_MONITORING_USERNAME, RABBITMQ_MONITORING_PASSWORD),
            verify=RABBITMQ_CA_CERT_PATH,
            timeout=5
        )
        response.raise_for_status()
        return {"status": "UP"} if response.json().get("status") == "ok" else {"status": "DOWN"}
    except requests.RequestException as e:
        logger.error("error occurred on RabbitMQ server's healthcheck: ", exc_info=True)
        return {"status": "DOWN"}

def dispatcher_healthcheck(app_name):
    try:
        response = requests.get(f"http://{app_name}.app.svc.cluster.local:8080/actuator/health", timeout=5)
        response.raise_for_status()
        data = response.json()
        return OrderedDict([
            ("status", data.get("status", "UNKNOWN")),
            ("components", data.get("components", {}))
        ])
    except requests.RequestException as e:
        logger.error("error occurred on dispatcher %s healthcheck: ", app_name, exc_info=True)
        return {"status": "DOWN"}

@app.route('/health', methods=['GET'])
def health():
    global_status = "UP"
    components = OrderedDict()

    # Fetch RabbitMQ health
    rabbitmq_health = rabbitmq_healthcheck()
    components["rabbitmq_server"] = rabbitmq_health
    if rabbitmq_health["status"] == "DOWN":
        global_status = "DOWN"

    # Fetch health from Spring apps
    for dispatcher_instance in DISPATCHER_INSTANCES:
        spring_health = dispatcher_healthcheck(dispatcher_instance)
        components[dispatcher_instance] = spring_health
        if spring_health["status"] == "DOWN":
            global_status = "DOWN"

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
    app.run(host="0.0.0.0", port=8080)
