import logging
import os

from flask import Flask, jsonify
import yaml

ENVIRONMENT = os.environ.get("ENVIRONMENT")

API_ENDPOINT = "/annuaire/api"
CLIENTS_ENDPOINT = f"{API_ENDPOINT}/clients"
HEALTH_ENDPOINT = "/annuaire/health"

VALUES_PATH = os.environ.get("VALUES_PATH", "/config/topology/values.yaml")
ANNUAIRE_CLIENTS_DATA_KEY = "ANNUAIRE_CLIENTS_DATA"

TOPOLOGY_ROOT_KEY = "annuaire"
TOPOLOGY_CLIENTS_KEY = "clients"

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
            return jsonify({"error": "Invalid perimeter", "valid_perimeters": sorted(VALID_PERIMETERS)}), 400
        filtered = [
            client
            for client in app.config[ANNUAIRE_CLIENTS_DATA_KEY]
            if client["perimeters"].get(perimeter) is True
        ]
        return jsonify(filtered)

    @app.get(HEALTH_ENDPOINT)
    def health_check():
        return jsonify({"status": "UP", "service": "SAMU Hub Annuaire"}), 200


def create_app():
    app = Flask(__name__)
    register_routes(app)
    clients = load_clients(VALUES_PATH)
    app.config[ANNUAIRE_CLIENTS_DATA_KEY] = build_annuaire_clients(clients)
    return app


if ENVIRONMENT == "production":
    app = create_app()
