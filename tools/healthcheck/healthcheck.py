import os
import requests
from flask import Flask, jsonify

app = Flask(__name__)

RABBITMQ_URL = os.getenv("RABBITMQ_URL", "http://rabbitmq-server.default.svc:15671")
RABBITMQ_MONITORING_USERNAME = os.getenv("RABBITMQ_MONITORING_USERNAME", "guest")
RABBITMQ_MONITORING_PASSWORD = os.getenv("RABBITMQ_MONITORING_PASSWORD", "guest")
RABBITMQ_CA_CERT_PATH = '/etc/ssl/certs/hubsante-rabbitmq-ca.crt'
# PROMETHEUS_URL = os.getenv("PROMETHEUS_URL", "http://prometheus-server.default.svc:9090")
SPRING_APPS = [
    {"name": "dispatcher-15-15-v1-5-service", "url": "http://spring-app-1/actuator/health"},
    {"name": "dispatcher-15-nexsis-v1-9-service", "url": "http://spring-app-2/actuator/health"},
]

def rabbitmq_healthcheck():
    try:
        response = requests.get(
            f"{RABBITMQ_URL}/rabbitmq/api/health/checks/alarms",
            auth=(RABBITMQ_MONITORING_USERNAME, RABBITMQ_MONITORING_PASSWORD),
            verify=RABBITMQ_CA_CERT_PATH
        )
        response.raise_for_status()
        return {"status": "UP"} if response.json().get("status") == "ok" else {"status": "DOWN"}
    except requests.RequestException as e:
        return {"status": "DOWN", "error": str(e)}

def dispatcher_healthcheck(app_name):
    try:
        response = requests.get(f"http://{app_name}.app.svc.cluster.local/actuator/health")
        response.raise_for_status()
        data = response.json()
        return {"status": data.get("status", "UNKNOWN"), "components": data.get("components", {})}
    except requests.RequestException as e:
        return {"status": "DOWN", "error": str(e)}

@app.route('/health', methods=['GET'])
def health():
    global_status = "UP"
    components = {}

    # Fetch RabbitMQ health
    rabbitmq_health = rabbitmq_healthcheck()
    components["rabbitmq_server"] = rabbitmq_health
    if rabbitmq_health["status"] == "DOWN":
        global_status = "DOWN"

    # Fetch health from Spring apps
    for app in SPRING_APPS:
        spring_health = dispatcher_healthcheck(app)
        components[app] = spring_health
        if spring_health["status"] == "DOWN":
            global_status = "DOWN"

    # Aggregate and return the result
    result = {"status": global_status, "components": components}
    return jsonify(result)

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8080)
