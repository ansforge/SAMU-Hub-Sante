import json
import logging
from flask import Flask, Response, request
from collections import OrderedDict
from prometheus_flask_exporter import PrometheusMetrics

from utils import remove_error_keys
from checks.status import Status
from checks.converter import ConverterHealthcheck
from checks.rabbitmq import RabbitMQHealthcheck
from checks.dispatcher import DispatchersHealthcheck
from checks.annuaire import AnnuaireHealthcheck
from checks.hubex_partners_shovels import HubexPartnersHealthcheck

app = Flask(__name__)
metrics = PrometheusMetrics(app)

checkers = [
    RabbitMQHealthcheck(),
    DispatchersHealthcheck(),
    ConverterHealthcheck(),
    AnnuaireHealthcheck(),
    HubexPartnersHealthcheck(),
]

logging.basicConfig(level=logging.INFO)

METRICS_ENDPOINT = "/metrics"
HEALTH_EXTERNAL_ENDPOINT = "/health"
DEFAULT_FLASK_HOST = "0.0.0.0"
DEFAULT_FLASK_PORT = 8080


@app.before_request
def update_metrics_before_scrapping():
    if request.path == METRICS_ENDPOINT:
        for checker in checkers:
            checker.perform_checks()


@app.route(HEALTH_EXTERNAL_ENDPOINT, methods=["GET"])
def health_external():
    global_status = Status.UP.value
    components = OrderedDict()

    for checker in checkers:
        health_statuses = checker.perform_checks()
        for component, status in health_statuses.items():
            components[component] = status
            if status["status"] == Status.DOWN.value:
                global_status = Status.DOWN.value

    # Aggregate and return the result
    result = OrderedDict([("status", global_status), ("components", components)])
    return Response(
        response=json.dumps(remove_error_keys(result)), mimetype="application/json"
    )


if __name__ == "__main__":
    app.run(host=DEFAULT_FLASK_HOST, port=DEFAULT_FLASK_PORT)
