from flask import Flask, jsonify, abort
import csv
import os

app = Flask(__name__)
CSV_DIR = "/config"

@app.get("/annuaire")
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
    data = clean_data(data)
    return jsonify(data)

def clean_data(data: list[dict]) -> list[dict]:
    """On enlève les lignes et les colonnes du csv qui ne nous intéressent pas"""
    columns_to_remove = ['CommonName', 'additionalPermissions', 'lrm_test']
    data_updated = []
    for row in data:
        if row.get('CommonName'):
            cleaned_row = {key: value for key, value in row.items() if key not in columns_to_remove}
            data_updated.append(cleaned_row)
    return data_updated