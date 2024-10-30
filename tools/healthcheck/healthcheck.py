import os
import requests
import socket
import ssl
from flask import Flask, jsonify

app = Flask(__name__)

RABBITMQ_URL = os.getenv("RABBITMQ_URL", "http://rabbitmq-server.default.svc:15671")
RABBITMQ_MONITORING_USERNAME = os.getenv("RABBITMQ_MONITORING_USERNAME", "guest")
RABBITMQ_MONITORING_PASSWORD = os.getenv("RABBITMQ_MONITORING_PASSWORD", "guest")
RABBITMQ_CA_CERT_PATH = '/etc/ssl/certs/hubsante-rabbitmq-ca.crt'
PROMETHEUS_URL = os.getenv("PROMETHEUS_URL", "http://prometheus-server.default.svc:9090")

def rabbitmq_healthcheck():
    response = requests.get(f"{RABBITMQ_URL}/rabbitmq/api/health/checks/alarms", auth=(RABBITMQ_MONITORING_USERNAME,RABBITMQ_MONITORING_PASSWORD), verify=RABBITMQ_CA_CERT_PATH)
    response.raise_for_status()

    data = response.json()
    rabbitmq_status = "unhealthy"
    if (data.get("status") == 'ok'):
        rabbitmq_status = "healthy"
    result = {"rabbitmq_healthcheck": rabbitmq_status}
    return result

def query_prometheus(query):
    response = requests.get(f"{PROMETHEUS_URL}/prometheus/api/v1/query", params={"query": query})

    # Check if response is successful
    response.raise_for_status()  # This will raise an error for HTTP errors

    data = response.json()
    result = "unhealthy"
    if (data.get("data", {}).get("result")):
        result = "healthy"
    return result

def check_service_health():
    # check if RabbitMQ is up
    rabbitmq_health_status = rabbitmq_healthcheck()
    # Check if all dispatchers are connectec
    dispatchers_connected_result = query_prometheus('sum(rabbitmq_queue_consumers{queue="dispatch"})/sum(up{job=~"dispatcher-.*-service"}) == 1')
    dispatchers_connected_status = {"all_dispatchers_connected": dispatchers_connected_result}
    # aggregate results
    return {**rabbitmq_health_status,**dispatchers_connected_status}

@app.route('/health', methods=['GET'])
def health():
    health_status = check_service_health()
    global_status = "healthy" if all(value == 'healthy' for value in health_status.values()) else "unhealthy"

    return jsonify({"global_status": global_status, "details": health_status})

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8080)
