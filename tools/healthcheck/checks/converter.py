from prometheus_client import REGISTRY, Gauge
from prometheus_client.core import GaugeMetricFamily
from prometheus_client.registry import Collector

from checks.checker import IChecker
from checks.status import Status
from http_client import http_session

from config import CONVERTER_HEALTH_URL

# Known converter statuses are returned unchanged; any unrecognized status falls back to DOWN.
# DEGRADED means the converter is reachable but cannot access its database.
CONVERTER_ACCEPTED_STATUSES = (Status.UP.value, Status.DEGRADED.value)


class ConverterMongodbCollector(Collector):
    """Exposes converter_mongodb_status only while the state of the database is
    known. A Gauge cannot drop its sample, so a collector is used: no series is
    published when the converter is unreachable, which is already covered by
    converter_status, so that a single alert is raised."""

    def __init__(self):
        self.value = None

    def collect(self):
        if self.value is None:
            return
        metric = GaugeMetricFamily(
            "converter_mongodb_status",
            "Statut de la connexion MongoDB du converter (1=UP, 0=DOWN)",
        )
        metric.add_metric([], self.value)
        yield metric


converter_mongodb_collector = ConverterMongodbCollector()
REGISTRY.register(converter_mongodb_collector)


def _mongodb_state(components):
    mongodb = components.get("mongodb")
    if isinstance(mongodb, dict):
        mongodb = mongodb.get("status")
    if mongodb is None:
        return None
    return 1 if mongodb == Status.UP.value else 0


class ConverterHealthcheck(IChecker):
    converter_status_metric = Gauge(
        "converter_status", "Statut du converter (1=joignable: UP ou DEGRADED, 0=DOWN)"
    )

    def perform_checks(self):
        response = http_session.get(CONVERTER_HEALTH_URL)
        response.raise_for_status()
        data = response.json()
        status = data.get("status", Status.UNKNOWN.value)
        if status not in CONVERTER_ACCEPTED_STATUSES:
            status = Status.DOWN.value
        self.converter_status_metric.set(0 if status == Status.DOWN.value else 1)
        converter_mongodb_collector.value = (
            None
            if status == Status.DOWN.value
            else _mongodb_state(data.get("components", {}))
        )
        return {"converter": {"status": status}}

    def check_failure_fallback(self):
        self.converter_status_metric.set(0)
        converter_mongodb_collector.value = None
        return {"converter": {"status": Status.DOWN.value}}
