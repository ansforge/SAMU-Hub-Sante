import json
import logging
from flask import Flask, Response, request
from collections import OrderedDict
from prometheus_flask_exporter import PrometheusMetrics

from utils import remove_error_keys
from checks.status import Status
from checks.converter import converter_healthcheck
from checks.rabbitmq import rabbitmq_healthcheck
from checks.dispatcher import dispatchers_healthcheck
from checks.annuaire import annuaire_healthcheck
from checks.hubex_partners_shovels import HubexPartnersHealthcheck

app = Flask(__name__)
metrics = PrometheusMetrics(app)

hubex_partners_checks = HubexPartnersHealthcheck()

logging.basicConfig(level=logging.INFO)

METRICS_ENDPOINT = "/metrics"
HEALTH_EXTERNAL_ENDPOINT = "/health"
DEFAULT_FLASK_HOST = "0.0.0.0"
DEFAULT_FLASK_PORT = 8080


@app.before_request
def update_metrics_before_scrapping():
    if request.path == METRICS_ENDPOINT:
        rabbitmq_healthcheck()
        dispatchers_healthcheck()
        converter_healthcheck()
        annuaire_healthcheck()
        hubex_partners_checks.hubex_partners_shovels_healthcheck()


@app.route(HEALTH_EXTERNAL_ENDPOINT, methods=["GET"])
def health_external():
    global_status = Status.UP.value
    components = OrderedDict()

    # Fetch RabbitMQ health
    rabbitmq_health = rabbitmq_healthcheck()
    components["rabbitmq_server"] = rabbitmq_health
    if rabbitmq_health["status"] == Status.DOWN.value:
        global_status = Status.DOWN.value

    dispatchers_health = dispatchers_healthcheck()
    for dispatcher_instance, spring_health in dispatchers_health.items():
        components[dispatcher_instance] = spring_health
        if spring_health["status"] == Status.DOWN.value:
            global_status = Status.DOWN.value

    partners_shovels_health = hubex_partners_checks.hubex_partners_shovels_healthcheck()
    for shovel_name, shovel_health in partners_shovels_health.items():
        components[shovel_name] = shovel_health
        if shovel_health["status"] == Status.DOWN.value:
            global_status = Status.DOWN.value

    converter_health = converter_healthcheck()
    components["converter"] = converter_health
    if converter_health["status"] == Status.DOWN.value:
        global_status = Status.DOWN.value

    annuaire_health = annuaire_healthcheck()
    components["annuaire"] = annuaire_health
    if annuaire_health["status"] == Status.DOWN.value:
        global_status = Status.DOWN.value

    # Aggregate and return the result
    result = OrderedDict([("status", global_status), ("components", components)])
    return Response(
        response=json.dumps(remove_error_keys(result)), mimetype="application/json"
    )


if __name__ == "__main__":
    app.run(host=DEFAULT_FLASK_HOST, port=DEFAULT_FLASK_PORT)
