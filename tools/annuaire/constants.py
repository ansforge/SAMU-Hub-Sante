import os

API_ENDPOINT = "/annuaire/api"
CLIENTS_ENDPOINT = f"{API_ENDPOINT}/clients"
# l'ingress ne route que /annuaire, la route reste interne au cluster
INTERNAL_CLIENTS_ENDPOINT = "/internal/clients"
HEALTH_ENDPOINT = "/annuaire/health"
SPECS_ENDPOINT = f"{API_ENDPOINT}/specs"
VALUES_PATH = os.environ.get("VALUES_PATH", "/config/clients/values.yaml")
ANNUAIRE_CLIENTS_DATA_KEY = "ANNUAIRE_CLIENTS_DATA"
INTERNAL_ANNUAIRE_CLIENTS_DATA_KEY = "INTERNAL_ANNUAIRE_CLIENTS_DATA"
ANNUAIRE_ROOT_KEY = "annuaire"
ANNUAIRE_CLIENTS_KEY = "clients"
