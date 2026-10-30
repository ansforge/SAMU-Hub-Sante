import logging
import os
import sys

from prometheus_client import Gauge

from checks.checker import IChecker
from checks.hubex_partners_shovels import parse_monitored_shovels_line
from checks.status import Status, aggregate_statuses
from http_client import http_session
from config import (
    QUEUES_URL,
    RABBITMQ_MONITORING_USERNAME,
    RABBITMQ_MONITORING_PASSWORD,
    RABBITMQ_CA_CERT_PATH,
)

# Queues made available to the Hubex partners (Hub Santé -> Hubex direction).
# The partner is considered connected when it has at least one consumer on them.
MONITORED_QUEUES_CONFIG_FILE_NAME = "monitored_hubex_queues.txt"


def build_queues_config_file_path():
    return os.path.abspath(
        os.path.join(os.path.dirname(__file__), "..", MONITORED_QUEUES_CONFIG_FILE_NAME)
    )


def parse_monitored_queues_config_file(config_file_path):
    queues = {}
    try:
        with open(config_file_path, "r", encoding="utf-8") as config_file:
            for line in config_file:
                if not line.strip():
                    continue
                vhost, vhost_queues = parse_monitored_shovels_line(line)
                queues[vhost] = vhost_queues
    except FileNotFoundError:
        sys.exit(f"{MONITORED_QUEUES_CONFIG_FILE_NAME} not found at {config_file_path}")
    except Exception as e:
        sys.exit(f"Error reading {MONITORED_QUEUES_CONFIG_FILE_NAME}: {e}")
    return queues


def check_queue_consumers(data, vhost, queue):
    def find_queue(element):
        if not isinstance(element, dict):
            return False
        return element.get("vhost") == vhost and element.get("name") == queue

    res = list(filter(find_queue, data))
    if len(res) == 0:
        logging.error(
            f"Missing queue '{queue}' in vhost '{vhost}' in RabbitMQ API response. Queues endpoint returned: {data}"
        )
        return {"status": Status.DOWN.value}
    if len(res) > 1:
        logging.error(
            f"Multiple queues found with name '{queue}' in vhost '{vhost}' in RabbitMQ API response. Queues endpoint returned: {data}"
        )
        return {"status": Status.DOWN.value}

    consumers = res[0].get("consumers")
    if not isinstance(consumers, int):
        logging.error(
            f"Invalid or missing 'consumers' field for queue '{queue}' in vhost '{vhost}': {consumers}"
        )
        return {"status": Status.DOWN.value}
    return {"status": Status.UP.value if consumers > 0 else Status.DOWN.value}


class HubexConsumersHealthcheck(IChecker):
    hubex_consumers_status_metric = Gauge(
        "hubex_consumers_status",
        "Statut des consumers des Hubex partenaires sur les queues du Hub (1=UP, 0=DOWN)",
        ["queue"],
    )

    QUEUES_MAP = {}

    def __init__(self):
        config_file_path = build_queues_config_file_path()
        self.QUEUES_MAP = parse_monitored_queues_config_file(config_file_path)
        for vhost, queues_list in self.QUEUES_MAP.items():
            for queue_name in queues_list:
                self.hubex_consumers_status_metric.labels(
                    queue=f"{vhost}-{queue_name}"
                ).set(0)

    def perform_checks(self):
        if not any(self.QUEUES_MAP.values()):
            return {}
        response = http_session.get(
            QUEUES_URL,
            auth=(RABBITMQ_MONITORING_USERNAME, RABBITMQ_MONITORING_PASSWORD),
            verify=RABBITMQ_CA_CERT_PATH,
        )
        response.raise_for_status()
        data = response.json()
        result = {}
        for vhost, queues_list in self.QUEUES_MAP.items():
            queue_statuses = []
            for queue in queues_list:
                logging.info(
                    f"Checking consumers of hubex partner on vhost {vhost} and queue {queue}"
                )
                queue_status = check_queue_consumers(data, vhost, queue)
                queue_label = f"{vhost}-{queue}"
                result[f"{queue_label}-consumers"] = queue_status
                queue_statuses.append(queue_status["status"])
                self.hubex_consumers_status_metric.labels(queue=queue_label).set(
                    1 if queue_status["status"] == Status.UP.value else 0
                )
            if queue_statuses:
                result[f"{vhost}-consumers"] = {
                    "status": aggregate_statuses(queue_statuses)
                }
        return result

    def check_failure_fallback(self):
        result = {}
        for vhost, queues_list in self.QUEUES_MAP.items():
            for queue in queues_list:
                queue_label = f"{vhost}-{queue}"
                result[f"{queue_label}-consumers"] = {"status": Status.DOWN.value}
                self.hubex_consumers_status_metric.labels(queue=queue_label).set(0)
            if queues_list:
                result[f"{vhost}-consumers"] = {"status": Status.DOWN.value}
        return result
