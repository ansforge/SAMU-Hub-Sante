import logging
import os

from flask import Flask, jsonify
import yaml

DEFAULT_FLASK_HOST = "0.0.0.0"
DEFAULT_FLASK_PORT = 8080

VALUES_FILE_PATH = os.environ.get(
    "VALUES_FILE_PATH", "/config/rabbitmq.clients-configuration.csv"
)

API_ENDPOINT = "/annuaire/api"
CLIENTS_ENDPOINT = f"{API_ENDPOINT}/clients"
HEALTH_ENDPOINT = "/annuaire/health"

VALUES_PATH = os.environ.get("VALUES_PATH", "/config/topology/values.yaml")
CLIENTS_DATA_KEY = "CLIENTS_DATA"
ANNUAIRE_CLIENTS_DATA_KEY = "ANNUAIRE_CLIENTS_DATA"

TOPOLOGY_ROOT_KEY = "annuaire"
TOPOLOGY_CLIENTS_KEY = "clients"

TOPOLOGY_TO_LEGACY_KEY = {
    "lrmPerimeterVersions": "P: 15-15",
    "smurPerimeterVersions": "P: 15-smur",
    "cisuPerimeterVersions": "P: 15-nexsis",
    "gpsPerimeterVersions": "P: 15-gps",
}

ANNUAIRE_TO_PERIMETER_KEY = {
    "lrm": "15-15",
    "cap": "15-cap",
    "portail": "15-portail",
    "cnr114": "15-cnr114",
    "cisu": "15-nexsis",
    "smur": "15-smur",
    "gps": "15-gps",
}


def load_clients(path: str) -> list[dict]:
    try:
        with open(path, encoding="utf-8") as f:
            data = yaml.safe_load(f)
        if TOPOLOGY_ROOT_KEY not in data:
            raise RuntimeError(f"Missing '{TOPOLOGY_ROOT_KEY}' key in {path}")
        if TOPOLOGY_CLIENTS_KEY not in data[TOPOLOGY_ROOT_KEY]:
            raise RuntimeError(
                f"Missing '{TOPOLOGY_ROOT_KEY}.{TOPOLOGY_CLIENTS_KEY}' key in {path}"
            )
        return data[TOPOLOGY_ROOT_KEY][TOPOLOGY_CLIENTS_KEY]
    except FileNotFoundError:
        logging.error(f"Values file not found: {path}")
        raise
    except RuntimeError as e:
        logging.error(f"Failed to load clients from {path}: {e}")
        raise
    except Exception as e:
        logging.error(f"Failed to load clients from {path}: {e}")
        raise RuntimeError(f"Failed to load clients from {path}: {e}") from e


def build_client_entry(client: dict) -> dict:
    entry = {
        "client_id": client["client_id"],
        "editor": client.get("editor", ""),
        "directCISU": client.get("directCISU", False),
    }
    for topo_key, legacy_key in TOPOLOGY_TO_LEGACY_KEY.items():
        if topo_key in client:
            entry[legacy_key] = client[topo_key]
    return entry


def resolve_perimeters(client: dict) -> dict:
    annuaire = client.get("annuaire")
    if not isinstance(annuaire, dict):
        return {}

    perimeters = {}
    for annuaire_key, perimeter_key in ANNUAIRE_TO_PERIMETER_KEY.items():
        perimeters[perimeter_key] = bool(annuaire.get(annuaire_key, False))

    return perimeters


def build_annuaire_client_entry(client: dict) -> dict:
    return {
        "client_id": client["client_id"],
        "client_name": client.get("client_name", ""),
        "client_type": client.get("client_type", ""),
        "editor": client.get("editor", ""),
        "directCISU": bool(client.get("directCISU", False)),
        "isLinkedToNexsis": bool(client.get("isLinkedToNexsis", False)),
        "perimeters": resolve_perimeters(client),
    }


def build_annuaire_clients(clients: list[dict]) -> list[dict]:
    annuaire_clients = []
    for client in clients:
        if isinstance(client.get("annuaire"), dict):
            annuaire_clients.append(build_annuaire_client_entry(client))
    return annuaire_clients


def register_routes(app):
    @app.get(API_ENDPOINT)
    def get_json():
        return jsonify(app.config[CLIENTS_DATA_KEY])

    @app.get(CLIENTS_ENDPOINT)
    def get_clients():
        return jsonify(app.config[ANNUAIRE_CLIENTS_DATA_KEY])

    @app.get(f"{CLIENTS_ENDPOINT}/<perimeter>")
    def get_clients_by_perimeter(perimeter):
        filtered = [
            client
            for client in app.config[ANNUAIRE_CLIENTS_DATA_KEY]
            if client["perimeters"].get(perimeter) is True
        ]
        return jsonify(filtered)

    @app.get(HEALTH_ENDPOINT)
    def health_check():
        return jsonify({"status": "UP", "service": "SAMU Hub Annuaire"}), 200


def create_app():
    app = Flask(__name__)
    register_routes(app)
    clients = load_clients(VALUES_PATH)
    app.config[CLIENTS_DATA_KEY] = [build_client_entry(c) for c in clients]
    app.config[ANNUAIRE_CLIENTS_DATA_KEY] = build_annuaire_clients(clients)
    return app


if ENVIRONMENT == "production":
    app = create_app()
