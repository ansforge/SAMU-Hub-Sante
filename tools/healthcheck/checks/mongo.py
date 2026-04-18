from prometheus_client import Gauge

from checks.checker import IChecker
from checks.status import Status

from pymongo import MongoClient
from config import MONGODB_URI, HTTP_TIMEOUT


class MongoDBHealthcheck(IChecker):
    mongodb_status_metric = Gauge("mongodb_status", "Statut de MongoDB (1=UP, 0=DOWN)")

    def perform_checks(self):
        mongo_client = MongoClient(MONGODB_URI, timeoutMS=HTTP_TIMEOUT * 1000)
        mongo_client.admin.command("ping")

        self.mongodb_status_metric.set(1)
        return {"mongodb": {"status": Status.UP.value}}

    def check_failure_fallback(self):
        self.mongodb_status_metric.set(0)
        return {"mongodb": {"status": Status.DOWN.value}}
