import json
import logging
from concurrent.futures import ThreadPoolExecutor

from flask import Flask, Response, request
from collections import OrderedDict
from prometheus_flask_exporter import PrometheusMetrics

from utils import remove_error_keys
from checks.status import Status, aggregate_statuses
from checks.platform_status import build_platform_status
from checks.converter import ConverterHealthcheck
from checks.rabbitmq import RabbitMQHealthcheck
from checks.dispatcher import DISPATCHERS_AGGREGATE_NAME, DispatchersHealthcheck
from checks.annuaire import AnnuaireHealthcheck
from checks.hubex_partners_shovels import HubexPartnersHealthcheck
from checks.hubex_consumers import HubexConsumersHealthcheck
from checks.mongo import MongoDBHealthcheck
import argparse

app = Flask(__name__)
metrics = PrometheusMetrics(app)

rabbitmq_server = RabbitMQHealthcheck()
dispatcher = DispatchersHealthcheck()
converter = ConverterHealthcheck()
annuaire = AnnuaireHealthcheck()
hubex_partners = HubexPartnersHealthcheck()
hubex_consumers = HubexConsumersHealthcheck()
mongodb = MongoDBHealthcheck()

external_checkers = [
    rabbitmq_server,
    dispatcher,
    converter,
    hubex_partners,
    hubex_consumers,
    mongodb,
]
internal_checkers = [
    rabbitmq_server,
    dispatcher,
    converter,
    annuaire,
    hubex_partners,
    hubex_consumers,
    mongodb,
]

logging.basicConfig(level=logging.INFO)

METRICS_ENDPOINT = "/metrics"
HEALTH_EXTERNAL_ENDPOINT = "/health"
HEALTH_INTERNAL_ENDPOINT = "/internal/health"
DEFAULT_FLASK_HOST = "0.0.0.0"
DEFAULT_FLASK_PORT = 8080


def _component_status(components, name):
    return components.get(name, {}).get("status", Status.UNKNOWN.value)


def _connection_status(components, vhosts, suffix):
    """Aggregated status of the Hubex connections of all the vhosts, or None if
    there is nothing to monitor."""
    statuses = [
        components[f"{vhost}-{suffix}"]["status"]
        for vhost in vhosts
        if f"{vhost}-{suffix}" in components
    ]
    return aggregate_statuses(statuses) if statuses else None


def compute_platform_status(components):
    fire_connections = {
        "fire_to_health": _connection_status(
            components, hubex_partners.SHOVELS_MAP, "shovels"
        ),
        "health_to_fire": _connection_status(
            components, hubex_consumers.QUEUES_MAP, "consumers"
        ),
    }
    return build_platform_status(
        rabbitmq=_component_status(components, "rabbitmq_server"),
        converter=_component_status(components, "converter"),
        mongodb=_component_status(components, "mongodb"),
        dispatchers_aggregate=_component_status(components, DISPATCHERS_AGGREGATE_NAME),
        dispatcher_statuses={
            name: _component_status(components, name)
            for name in dispatcher.dispatcher_instances
        },
        dispatcher_perimeters=dispatcher.dispatcher_perimeters,
        fire_connections={
            name: status
            for name, status in fire_connections.items()
            if status is not None
        },
    )


def compute_globale_status(custom_checkers):
    components = OrderedDict()

    with ThreadPoolExecutor() as executor:
        futures = [
            (checker, executor.submit(checker.check_wrapper))
            for checker in custom_checkers
        ]
        for _, future in futures:
            components.update(future.result())

    platform = compute_platform_status(components)
    result = OrderedDict(
        [
            ("status", platform["status"]),
            ("components", components),
            ("perimeters", platform["perimeters"]),
            ("technical", platform["technical"]),
        ]
    )
    return remove_error_keys(result)


@app.before_request
def update_metrics_before_scrapping():
    if request.path == METRICS_ENDPOINT:
        with ThreadPoolExecutor() as executor:
            futures = [
                executor.submit(checker.check_wrapper) for checker in internal_checkers
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
    parser = argparse.ArgumentParser()
    parser.add_argument("--port", type=int, default=DEFAULT_FLASK_PORT)
    args = parser.parse_args()
    app.run(host=DEFAULT_FLASK_HOST, port=args.port)
