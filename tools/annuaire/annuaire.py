from flask import Flask, jsonify, abort
import csv
import os

app = Flask(__name__)
CSV_DIR = "/config"

@app.get("/api/annuaire")
def get_json():
    filename = "rabbitmq.clients-configuration.csv"
    path = os.path.join(CSV_DIR, filename)
    if not os.path.exists(path):
        abort(500, description="Fichier CSV introuvable")
    try:
        with open(path, newline='', encoding='utf-8') as csvfile:
            reader = csv.DictReader(csvfile)
            data = list(reader)
    except Exception as e:
        abort(500, description=f"Erreur lors de la lecture du CSV: {e}")
    data = select_columns(data)
    return jsonify(data)

def select_columns(data: list[dict]) -> list[dict]:
    headers_columns_to_keep = ['client_id', 'editor', 'P: 15-15', 'P: 15-smur', 'P: 15-nexsis', 'P: 15-gps', 'directCISU']
    data_updated = []
    for row in data:
        row_updated = {key: value for key, value in row.items() if key in headers_columns_to_keep}
        data_updated.append(row_updated)
    return data_updated