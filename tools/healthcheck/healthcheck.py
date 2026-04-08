import json
import logging
from concurrent.futures import ThreadPoolExecutor

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

rabbitmq_server = RabbitMQHealthcheck()
dispatcher = DispatchersHealthcheck()
converter = ConverterHealthcheck()
annuaire = AnnuaireHealthcheck()
hubex_partners = HubexPartnersHealthcheck()

external_checkers = [rabbitmq_server, dispatcher, converter, hubex_partners]
internal_checkers = [rabbitmq_server, dispatcher, converter, annuaire, hubex_partners]

logging.basicConfig(level=logging.INFO)

METRICS_ENDPOINT = "/metrics"
HEALTH_EXTERNAL_ENDPOINT = "/health"
HEALTH_INTERNAL_ENDPOINT = "/internal/health"
DEFAULT_FLASK_HOST = "0.0.0.0"
DEFAULT_FLASK_PORT = 8080


def compute_globale_status(custom_checkers):
    global_status = Status.UP.value
    components = OrderedDict()

    with ThreadPoolExecutor() as executor:
        futures = [
            (checker, executor.submit(checker.check_wrapper))
            for checker in custom_checkers
        ]
        for _, future in futures:
            health_statuses = future.result()
            for component, status in health_statuses.items():
                components[component] = status
                if status["status"] == Status.DOWN.value:
                    global_status = Status.DOWN.value

    result = OrderedDict([("status", global_status), ("components", components)])
    return remove_error_keys(result)


@app.before_request
def update_metrics_before_scrapping():
    if request.path == METRICS_ENDPOINT:
        with ThreadPoolExecutor() as executor:
            futures = [
                executor.submit(checker.check_wrapper)
                for checker in internal_checkers
            ]
            for future in futures:
                future.result()


@app.route(HEALTH_EXTERNAL_ENDPOINT, methods=["GET"])
def health_external():
    result = compute_globale_status(external_checkers)
    return Response(response=json.dumps(result), mimetype="application/json")


@app.route(HEALTH_INTERNAL_ENDPOINT, methods=["GET"])
def health_internal():
    result = compute_globale_status(internal_checkers)
    return Response(response=json.dumps(result), mimetype="application/json")


if __name__ == "__main__":
    app.run(host=DEFAULT_FLASK_HOST, port=DEFAULT_FLASK_PORT)
