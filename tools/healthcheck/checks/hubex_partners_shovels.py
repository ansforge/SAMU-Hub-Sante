import logging
import os
import sys

from prometheus_client import Gauge

from checks.checker import IChecker
from checks.status import Status
from http_client import http_session
from config import (
    RABBITMQ_URL,
    RABBITMQ_MONITORING_USERNAME,
    RABBITMQ_MONITORING_PASSWORD,
    RABBITMQ_CA_CERT_PATH,
)

SHOVEL_STATUS_URL = f"{RABBITMQ_URL}/rabbitmq/api/shovels"
MONITORED_SHOVEL_CONFIG_FILE_NAME = "monitored_partners_shovels.txt"
EXPECTED_SHOVEL_STATUS = "running"


def build_shovels_config_file_path():
    return os.path.abspath(
        os.path.join(os.path.dirname(__file__), "..", MONITORED_SHOVEL_CONFIG_FILE_NAME)
    )


def parse_monitored_shovels_line(line):
    line = line.strip()
    vhost, src_queues_str = line.split(";", 1)
    src_queues = [q.strip() for q in src_queues_str.split(",") if q.strip()]
    return vhost.strip(), src_queues


def parse_monitored_shovels_config_file(config_file_path):
    shovels = {}
    try:
        with open(config_file_path, "r", encoding="utf-8") as config_file:
            for line in config_file:
                vhost, src_queues = parse_monitored_shovels_line(line)
                shovels[vhost] = src_queues
    except FileNotFoundError:
        sys.exit(f"{MONITORED_SHOVEL_CONFIG_FILE_NAME} not found at {config_file_path}")
    except Exception as e:
        sys.exit(f"Error reading {MONITORED_SHOVEL_CONFIG_FILE_NAME}: {e}")
    return shovels


def check_shovel_response(data, vhost, src_queue):
    def find_shovel(element):
        if not isinstance(element, dict):
            return False
        if "vhost" not in element or "src_queue" not in element:
            return False
        return element["vhost"] == vhost and element["src_queue"] == src_queue

    res = list(filter(find_shovel, data))
    if len(res) == 1:
        if res[0]["blocked_status"] != EXPECTED_SHOVEL_STATUS:
            return {"status": Status.DOWN.value}
        return {"status": Status.UP.value}
    elif len(res) == 0:
        logging.error(
            f"Missing shovel with vhost '{vhost}' and src_queue '{src_queue}' in RabbitMQ API response. Shovel status endpoint returned: {data}"
        )
        return {"status": Status.DOWN.value}
    else:
        logging.error(
            f"Mutliple shovels found with vhost '{vhost}' and src_queue '{src_queue}' in RabbitMQ API response. This is a config error. Shovel status endpoint returned: {data}"
        )
        return {"status": Status.DOWN.value}


class HubexPartnersHealthcheck(IChecker):
    hubex_partners_status_metric = Gauge(
        "hubex_partners_status",
        "Statut des shovels connectés aux Hubex partenaires (1=UP, 0=DOWN)",
        ["shovel"],
    )

    SHOVELS_MAP = {}

    def __init__(self):
        config_file_path = build_shovels_config_file_path()
        self.SHOVELS_MAP = parse_monitored_shovels_config_file(config_file_path)
        for vhost, shovels_list in self.SHOVELS_MAP.items():
            for queue_name in shovels_list:
                self.hubex_partners_status_metric.labels(
                    shovel=f"{vhost}-{queue_name}"
                ).set(0)

    def perform_checks(self):
        logging.info(
            f"Checking health of hubex partners connexions: {self.SHOVELS_MAP}"
        )

        response = http_session.get(
            SHOVEL_STATUS_URL,
            auth=(RABBITMQ_MONITORING_USERNAME, RABBITMQ_MONITORING_PASSWORD),
            verify=RABBITMQ_CA_CERT_PATH,
        )
        response.raise_for_status()
        data = response.json()
        result = {}
        for vhost, shovels_list in self.SHOVELS_MAP.items():
            for shovel in shovels_list:
                shovel_status = check_shovel_response(data, vhost, shovel)
                shovel_label = f"{vhost}-{shovel}"
                result[shovel_label] = shovel_status
                self.hubex_partners_status_metric.labels(shovel=shovel_label).set(
                    1 if shovel_status["status"] == Status.UP.value else 0
                )
        return result

    def check_failure_fallback(self):
        result = {}
        for vhost, shovels_list in self.SHOVELS_MAP.items():
            for shovel in shovels_list:
                shovel_label = f"{vhost}-{shovel}"
                result[shovel_label] = {"status": Status.DOWN.value}
                self.hubex_partners_status_metric.labels(shovel=shovel_label).set(0)
        return result
