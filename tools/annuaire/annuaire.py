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
HEALTH_ENDPOINT = "/annuaire/health"

VALUES_PATH = os.environ.get("VALUES_PATH", "/config/topology/values.yaml")
CLIENTS_DATA_KEY = "CLIENTS_DATA"

TOPOLOGY_ROOT_KEY = "annuaire"
TOPOLOGY_CLIENTS_KEY = "clients"

TOPOLOGY_TO_LEGACY_KEY = {
    "lrmPerimeterVersions": "P: 15-15",
    "smurPerimeterVersions": "P: 15-smur",
    "cisuPerimeterVersions": "P: 15-nexsis",
    "gpsPerimeterVersions": "P: 15-gps",
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


def register_routes(app):
    @app.get(API_ENDPOINT)
    def get_json():
        return jsonify(app.config[CLIENTS_DATA_KEY])

    @app.get(HEALTH_ENDPOINT)
    def health_check():
        return jsonify({"status": "UP", "service": "SAMU Hub Annuaire"}), 200


def create_app():
    app = Flask(__name__)
    register_routes(app)
    clients = load_clients(VALUES_PATH)
    app.config[CLIENTS_DATA_KEY] = [build_client_entry(c) for c in clients]
    return app


if ENVIRONMENT == "production":
    app = create_app()
