import logging
import os
from concurrent.futures import ThreadPoolExecutor

import requests
from prometheus_client import Gauge
from collections import OrderedDict

from checks.checker import IChecker
from checks.status import Status
from http_client import http_session

DISPATCHER_INSTANCES_ENV_VAR = os.getenv("DISPATCHER_INSTANCES")
DISPATCHER_INSTANCES = (
    DISPATCHER_INSTANCES_ENV_VAR.split(",") if DISPATCHER_INSTANCES_ENV_VAR else []
)


class DispatchersHealthcheck(IChecker):
    dispatcher_status_metric = Gauge(
        "dispatcher_status", "Statut des dispatchers (1=UP, 0=DOWN)", ["dispatcher"]
    )

    def __init__(self):
        # Initialize the metric for each dispatcher to DOWN by default
        for dispatcher in DISPATCHER_INSTANCES:
            self.dispatcher_status_metric.labels(dispatcher=dispatcher).set(0)

    def perform_checks(self):
        result = {}
        with ThreadPoolExecutor() as executor:
            futures = [
                (instance, executor.submit(self.single_dispatcher_healthcheck, instance))
                for instance in DISPATCHER_INSTANCES
            ]
            for instance, future in futures:
                result[instance] = future.result()
        return result

    def check_failure_fallback(self):
        result = {}
        for dispatcher_instance in DISPATCHER_INSTANCES:
            result[dispatcher_instance] = {"status": Status.DOWN.value}
            self.dispatcher_status_metric.labels(dispatcher=dispatcher_instance).set(0)
        return result

    def single_dispatcher_healthcheck(self, app_name):
        logging.info(f"Checking health of dispatcher instance: {app_name}")
        try:
            dispatcher_health_url = (
                f"http://{app_name}.app.svc.cluster.local:8080/actuator/health"
            )
            response = http_session.get(dispatcher_health_url)
            response.raise_for_status()
            data = response.json()
            status = data.get("status", Status.UNKNOWN.value)
            self.dispatcher_status_metric.labels(dispatcher=app_name).set(
                1 if status == Status.UP.value else 0
            )
            return OrderedDict(
                [("status", status), ("components", data.get("components", {}))]
            )
        except requests.RequestException:
            logging.error(
                "Error occurred on dispatcher %s healthcheck: ", app_name, exc_info=True
            )
            self.dispatcher_status_metric.labels(dispatcher=app_name).set(0)
            return {"status": Status.DOWN.value}
