import logging
from flask import Flask, jsonify, abort
import csv
import os

CSV_DIR = "/config"
CSV_DATA_KEY = 'CSV_DATA'
CSV_FILENAME = "rabbitmq.clients-configuration.csv"

def create_app() :    
    app = Flask(__name__)
    csv_data = parse_csv(CSV_FILENAME)
    if csv_data is None:
        raise RuntimeError("Erreur : impossible de charger le fichier CSV au démarrage.")
    app.config[CSV_DATA_KEY] = select_columns(csv_data)
    return app

def parse_csv(filename):
    path = os.path.join(CSV_DIR, filename)
    if not os.path.exists(path):
        logging.error(f"Fichier CSV introuvable : {path}")
        raise FileNotFoundError(f"Fichier CSV introuvable : {filename}")
    try:
        with open(path, newline='', encoding='utf-8') as csvfile:
            reader = csv.DictReader(csvfile)
            return list(reader)
    except Exception as e:
        logging.error(f"Erreur lors de la lecture du fichier CSV '{filename}': {e}")
        raise RuntimeError(f"Erreur lors de la lecture du CSV: {e}")

def select_columns(data: list[dict]) -> list[dict]:
    headers_columns_to_keep = ['client_id', 'editor', 'P: 15-15', 'P: 15-smur', 'P: 15-nexsis', 'P: 15-gps', 'directCISU']
    data_updated = []
    for row in data:
        row_updated = {key: value for key, value in row.items() if key in headers_columns_to_keep}
        data_updated.append(row_updated)
    return data_updated

app = create_app()

@app.get("/annuaire/api")
def get_json():
    return jsonify(app.config[CSV_DATA_KEY])