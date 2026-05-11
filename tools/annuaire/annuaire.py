import logging
from flask import Flask, jsonify
import csv
import os
import argparse
import yaml

DEFAULT_FLASK_HOST = "0.0.0.0"
DEFAULT_FLASK_PORT = 8080

VALUES_FILE_PATH = os.environ.get(
    "VALUES_FILE_PATH", "/config/rabbitmq.clients-configuration.csv"
)

CSV_DATA_KEY = "CSV_DATA"
API_ENDPOINT = "/annuaire/api"
HEALTH_ENDPOINT = "/annuaire/health"
CSV_NOT_FOUND_MSG = "Fichier CSV introuvable"

VALUES_DIR = "/config/topology"
VALUES_FILENAME = "values.yaml"
DATA_KEY = "DATA"

TOPOLOGY_TO_LEGACY_KEY = {
    "lrmPerimeterVersions":  "P: 15-15",
    "smurPerimeterVersions": "P: 15-smur",
    "cisuPerimeterVersions": "P: 15-nexsis",
    "gpsPerimeterVersions":  "P: 15-gps",
}

HEADERS_COLUMNS_TO_KEEP = [
    "client_id",
    "editor",
    "P: 15-15",
    "P: 15-smur",
    "P: 15-nexsis",
    "P: 15-gps",
    "directCISU",
]


def load_clients(path: str) -> list[dict]:
    with open(path) as f:
        data = yaml.safe_load(f)
    return data["hubsante-topology"]["clients"]


def build_client_entry(c: dict) -> dict:
    entry = {
        "client_id":  c["client_id"],
        "editor":     c.get("editor", ""),
        "directCISU": c.get("directCISU", False),
    }
    for topo_key, legacy_key in TOPOLOGY_TO_LEGACY_KEY.items():
        if topo_key in c:
            entry[legacy_key] = c[topo_key]
    return entry


def create_app():
    app = Flask(__name__)
    register_routes(app)
    csv_data = parse_csv(VALUES_FILE_PATH)
    if csv_data is None:
        raise RuntimeError(
            "Erreur : impossible de charger le fichier CSV au démarrage."
        )
    app.config[CSV_DATA_KEY] = select_columns(csv_data)
    return app


def parse_csv(filename):
    path = VALUES_FILE_PATH
    if not os.path.exists(path):
        logging.error(f"Fichier CSV introuvable : {path}")
        raise FileNotFoundError(f"{CSV_NOT_FOUND_MSG} : {filename}")
    try:
        with open(path, newline="", encoding="utf-8") as csvfile:
            reader = csv.DictReader(csvfile, delimiter=";")
            return list(reader)
    except Exception as e:
        logging.error(f"Erreur lors de la lecture du fichier CSV '{filename}': {e}")
        raise RuntimeError(f"Erreur lors de la lecture du CSV: {e}")


def select_columns(data: list[dict]) -> list[dict]:
    data_updated = []
    for row in data:
        row_updated = {
            key: value for key, value in row.items() if key in HEADERS_COLUMNS_TO_KEEP
        }
        data_updated.append(row_updated)
    return data_updated


def register_routes(app):
    @app.get(API_ENDPOINT)
    def get_json():
        return jsonify(app.config[CSV_DATA_KEY])

    @app.get(HEALTH_ENDPOINT)
    def health_check():
        return jsonify({"status": "UP", "service": "SAMU Hub Annuaire"}), 200


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--port", type=int, default=DEFAULT_FLASK_PORT)
    args = parser.parse_args()
    app = create_app()
    app.run(host=DEFAULT_FLASK_HOST, port=args.port)
