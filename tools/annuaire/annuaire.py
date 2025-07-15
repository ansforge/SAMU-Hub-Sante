from flask import Flask, jsonify, abort
import csv
import os

app = Flask(__name__)
CSV_DIR = "/config" # Le fichier CSV sera monté dans ce répertoire via une ConfigMap

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
    return jsonify(data)