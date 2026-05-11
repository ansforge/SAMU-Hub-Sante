import logging
from flask import Flask, jsonify
import os
import yaml

ENVIRONMENT = os.environ.get("ENVIRONMENT")

API_ENDPOINT = "/annuaire/api"
HEALTH_ENDPOINT = "/annuaire/health"

VALUES_DIR = "/config/topology"
VALUES_FILENAME = "values.yaml"
DATA_KEY = "DATA"

TOPOLOGY_TO_LEGACY_KEY = {
    "lrmPerimeterVersions": "P: 15-15",
    "smurPerimeterVersions": "P: 15-smur",
    "cisuPerimeterVersions": "P: 15-nexsis",
    "gpsPerimeterVersions": "P: 15-gps",
}


def load_clients(path: str) -> list[dict]:
    try:
        with open(path) as f:
            data = yaml.safe_load(f)
        return data["hubsante-topology"]["clients"]
    except FileNotFoundError:
        logging.error(f"Values file not found: {path}")
        raise
    except Exception as e:
        logging.error(f"Failed to load clients from {path}: {e}")
        raise RuntimeError(f"Failed to load clients from {path}: {e}") from e


def build_client_entry(c: dict) -> dict:
    entry = {
        "client_id": c["client_id"],
        "editor": c.get("editor", ""),
        "directCISU": c.get("directCISU", False),
    }
    for topo_key, legacy_key in TOPOLOGY_TO_LEGACY_KEY.items():
        if topo_key in c:
            entry[legacy_key] = c[topo_key]
    return entry


def create_app():
    app = Flask(__name__)
    register_routes(app)
    path = os.path.join(VALUES_DIR, VALUES_FILENAME)
    clients = load_clients(path)
    app.config[DATA_KEY] = [build_client_entry(c) for c in clients]
    return app


def register_routes(app):
    @app.get(API_ENDPOINT)
    def get_json():
        return jsonify(app.config[DATA_KEY])

    @app.get(HEALTH_ENDPOINT)
    def health_check():
        return jsonify({"status": "UP", "service": "SAMU Hub Annuaire"}), 200


if ENVIRONMENT == "production":
    app = create_app()
