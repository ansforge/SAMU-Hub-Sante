import os

API_ENDPOINT = "/annuaire/api"
CLIENTS_ENDPOINT = f"{API_ENDPOINT}/clients"
HEALTH_ENDPOINT = "/annuaire/health"
SPECS_ENDPOINT = f"{API_ENDPOINT}/specs"
VALUES_PATH = os.environ.get("VALUES_PATH", "/config/clients/values.yaml")
ANNUAIRE_CLIENTS_DATA_KEY = "ANNUAIRE_CLIENTS_DATA"
ANNUAIRE_ROOT_KEY = "annuaire"
ANNUAIRE_CLIENTS_KEY = "clients"
