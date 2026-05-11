import logging
from flask import Flask, jsonify
import os
import argparse
import yaml

DEFAULT_FLASK_HOST = "0.0.0.0"
DEFAULT_FLASK_PORT = 8080

VALUES_FILE_PATH = os.environ.get(
    "VALUES_FILE_PATH", "/config/rabbitmq.clients-configuration.csv"
)

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
        raise


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


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--port", type=int, default=DEFAULT_FLASK_PORT)
    args = parser.parse_args()
    app = create_app()
    app.run(host=DEFAULT_FLASK_HOST, port=args.port)
