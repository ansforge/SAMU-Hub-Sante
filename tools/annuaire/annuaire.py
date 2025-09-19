import logging
from flask import Flask, jsonify, abort
import csv
import os

ENVIRONMENT = os.environ.get("ENVIRONMENT")

CSV_DIR = "/config"
CSV_DATA_KEY = "CSV_DATA"
CSV_FILENAME = "rabbitmq.clients-configuration.csv"
API_ENDPOINT = "/annuaire/api"
HEALTH_ENDPOINT = "/annuaire/health"
CSV_NOT_FOUND_MSG = "Fichier CSV introuvable"

HEADERS_COLUMNS_TO_KEEP = [
    "client_id",
    "editor",
    "P: 15-15",
    "P: 15-smur",
    "P: 15-nexsis",
    "P: 15-gps",
    "directCISU",
]


def create_app(csv_dir=CSV_DIR):
    app = Flask(__name__)
    register_routes(app)
    csv_data = parse_csv(CSV_FILENAME, csv_dir)
    if csv_data is None:
        raise RuntimeError(
            "Erreur : impossible de charger le fichier CSV au démarrage."
        )
    app.config[CSV_DATA_KEY] = select_columns(csv_data)
    return app


def parse_csv(filename, csv_dir=CSV_DIR):
    path = os.path.join(csv_dir, filename)
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


if ENVIRONMENT == "production":
    app = create_app()
