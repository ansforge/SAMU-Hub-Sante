import logging
import os
import yaml

from flask import Flask, jsonify
from flask_cors import CORS

API_ENDPOINT = "/annuaire/api"
CLIENTS_ENDPOINT = f"{API_ENDPOINT}/clients"
HEALTH_ENDPOINT = "/annuaire/health"

VALUES_PATH = os.environ.get("VALUES_PATH", "/config/clients/values.yaml")
ANNUAIRE_CLIENTS_DATA_KEY = "ANNUAIRE_CLIENTS_DATA"

ANNUAIRE_ROOT_KEY = "annuaire"
ANNUAIRE_CLIENTS_KEY = "clients"

ANNUAIRE_TO_PERIMETER_KEY = {
    "lrm": "15-15",
    "cap": "15-cap",
    "portail": "15-portail",
    "cnr114": "15-cnr114",
    "cisu": "15-nexsis",
    "smur": "15-smur",
    "gps": "15-gps",
}

VALID_PERIMETERS = frozenset(ANNUAIRE_TO_PERIMETER_KEY.values())


def load_clients(path: str) -> list[dict]:
    try:
        with open(path, encoding="utf-8") as f:
            data = yaml.safe_load(f)
        if ANNUAIRE_ROOT_KEY not in data:
            raise RuntimeError(f"Missing '{ANNUAIRE_ROOT_KEY}' key in {path}")
        if ANNUAIRE_CLIENTS_KEY not in data[ANNUAIRE_ROOT_KEY]:
            raise RuntimeError(
                f"Missing '{ANNUAIRE_ROOT_KEY}.{ANNUAIRE_CLIENTS_KEY}' key in {path}"
            )
        return data[ANNUAIRE_ROOT_KEY][ANNUAIRE_CLIENTS_KEY]
    except FileNotFoundError:
        logging.error(f"Values file not found: {path}")
        raise
    except RuntimeError as e:
        logging.error(f"Failed to load clients from {path}: {e}")
        raise
    except Exception as e:
        logging.error(f"Failed to load clients from {path}: {e}")
        raise RuntimeError(f"Failed to load clients from {path}: {e}") from e


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
        "perimeters": resolve_perimeters(client),
    }


def build_annuaire_clients(clients: list[dict]) -> list[dict]:
    return [
        build_annuaire_client_entry(c)
        for c in clients
        if isinstance(c.get("annuaire"), dict)
    ]


def register_routes(app):
    @app.get(CLIENTS_ENDPOINT)
    def get_clients():
        return jsonify(app.config[ANNUAIRE_CLIENTS_DATA_KEY])

    @app.get(f"{CLIENTS_ENDPOINT}/<perimeter>")
    def get_clients_by_perimeter(perimeter):
        if perimeter not in VALID_PERIMETERS:
            return jsonify(
                {
                    "error": "Invalid perimeter",
                    "valid_perimeters": sorted(VALID_PERIMETERS),
                }
            ), 400
        filtered = [
            client
            for client in app.config[ANNUAIRE_CLIENTS_DATA_KEY]
            if client["perimeters"].get(perimeter) is True
        ]
        return jsonify(filtered)

    @app.get(HEALTH_ENDPOINT)
    def health_check():
        return jsonify({"status": "UP", "service": "SAMU Hub Annuaire"}), 200


def get_allowed_origins() -> list[str] | None:
    ALLOWED_ORIGINS = os.getenv("ALLOWED_ORIGINS")
    if ALLOWED_ORIGINS:
        return ALLOWED_ORIGINS.split(",")
    else:
        return None


def create_app():
    app = Flask(__name__)
    allowed_origins = get_allowed_origins()
    if allowed_origins:
        CORS(app, origins=allowed_origins)
    register_routes(app)
    clients = load_clients(VALUES_PATH)
    app.config[ANNUAIRE_CLIENTS_DATA_KEY] = build_annuaire_clients(clients)
    return app
