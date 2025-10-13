import requests
import logging
import os
import sys
from prometheus_client import Gauge

from checks.status import Status
from config import (
    HTTP_TIMEOUT,
    RABBITMQ_URL,
    RABBITMQ_MONITORING_USERNAME,
    RABBITMQ_MONITORING_PASSWORD,
    RABBITMQ_CA_CERT_PATH,
)

RABBITMQ_SHOVEL_STATUS_URL = f"{RABBITMQ_URL}/rabbitmq/api/shovels"
MONITORED_SHOVEL_CONFIG_FILE_NAME = "monitored_partners_shovels.txt"


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
        sys.exit(f"monitored_partners_shovels.txt not found at {config_file_path}")
    except Exception as e:
        sys.exit(f"Error reading monitored_partners_shovels.txt: {e}")
    return shovels


EXPECTED_SHOVEL_STATUS = "running"

hubex_partners_status_metric = Gauge(
    "hubex_partners_status",
    "Statut des shovels connectés aux Hubex partenaires (1=UP, 0=DOWN)",
    ["shovel"],
)

config_file_path = build_shovels_config_file_path()
SHOVELS_MAP = parse_monitored_shovels_config_file(config_file_path)

for vhost, shovels_list in SHOVELS_MAP.items():
    for queue_name in shovels_list:
        hubex_partners_status_metric.labels(shovel=f"{vhost}-{queue_name}").set(0)


def check_shovel_response(response, vhost, src_queue):
    def find_shovel(element):
        return element["vhost"] == vhost and element["src_queue"] == src_queue

    res = list(filter(find_shovel, response))
    if len(res) == 1:
        if res[0]["blocked_status"] != EXPECTED_SHOVEL_STATUS:
            return {"status": Status.DOWN.value}
        return {"status": Status.UP.value}
    elif len(res) == 0:
        logging.error("Missing shovel in RabbitMQ API response")
        return {"status": Status.DOWN.value}
    else:
        logging.error("Mutliple shovels found in RabbitMQ API response. Config Error")
        return {"status": Status.DOWN.value}


def hubex_partners_shovels_healthcheck():
    logging.info(f"Checking health of hubex partners connexions: {SHOVELS_MAP}")
    try:
        response = requests.get(
            RABBITMQ_SHOVEL_STATUS_URL,
            auth=(RABBITMQ_MONITORING_USERNAME, RABBITMQ_MONITORING_PASSWORD),
            verify=RABBITMQ_CA_CERT_PATH,
            timeout=HTTP_TIMEOUT,
        )
        response.raise_for_status()
        result = {}
        for vhost, shovels_list in SHOVELS_MAP.items():
            for shovel in shovels_list:
                shovel_status = check_shovel_response(response, vhost, shovel)
                shovel_label = f"{vhost}-{shovel}"
                result[shovel_label] = shovel_status
                hubex_partners_status_metric.labels(shovel=shovel_label).set(
                    1 if shovel_status["status"] == Status.OK.value else 0
                )
        return result
    except requests.RequestException:
        logging.error("Error occurred on Hubex partners healthcheck: ", exc_info=True)
        result = {}
        for vhost, shovels_list in SHOVELS_MAP.items():
            for shovel in shovels_list:
                shovel_label = f"{vhost}-{shovel}"
                result[shovel_label] = {"status": Status.DOWN.value}
                hubex_partners_status_metric.labels(shovel=shovel_label).set(0)
        return result
