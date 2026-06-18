import logging
import os
import yaml
from http import HTTPStatus

from flask_cors import CORS
from flask import Response, jsonify, redirect, make_response
from flask_openapi3 import Info, OpenAPI, Tag
from models import (
    ErrorResponse,
    Perimeter,
    Client,
    PerimeterPath,
    ClientsResponse,
    PERIMETER_TO_ANNUAIRE_KEY_MAP,
)
from pydantic import ValidationError

from constants import (
    ANNUAIRE_ROOT_KEY,
    ANNUAIRE_CLIENTS_KEY,
    CLIENTS_ENDPOINT,
    ANNUAIRE_CLIENTS_DATA_KEY,
    SPECS_ENDPOINT,
    HEALTH_ENDPOINT,
    VALUES_PATH,
)


def validation_error_callback(e: ValidationError):
    resp = make_response(
        jsonify(
            {
                "error": "Invalid perimeter",
                "valid_perimeters": list(Perimeter),
            }
        )
    )
    resp.headers["Content-Type"] = "application/json"
    resp.status_code = HTTPStatus.BAD_REQUEST
    return resp


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


def resolve_perimeters(annuaire: dict) -> dict[Perimeter, bool]:
    return {
        p: bool(annuaire.get(key, False))
        for p, key in PERIMETER_TO_ANNUAIRE_KEY_MAP.items()
    }


def build_annuaire_client_entry(client: dict) -> Client:
    return Client(
        client_id=client["client_id"],
        client_name=client.get("client_name", ""),
        client_type=client.get("client_type", ""),
        perimeters=resolve_perimeters(client.get(ANNUAIRE_ROOT_KEY, {})),
    )


def build_annuaire_clients(clients: list[dict]) -> list[Client]:
    return [
        build_annuaire_client_entry(c)
        for c in clients
        if isinstance(c.get(ANNUAIRE_ROOT_KEY), dict)
    ]


clients_tag = Tag(name="Clients", description="Annuaire des clients par périmètre")


def register_routes(app: OpenAPI) -> None:
    @app.get(CLIENTS_ENDPOINT, tags=[clients_tag], responses={200: ClientsResponse})
    def get_clients() -> ClientsResponse:
        """Lister tous les clients de l'annuaire"""
        clients: list[Client] = app.config[ANNUAIRE_CLIENTS_DATA_KEY]
        return jsonify([c.model_dump(by_alias=True) for c in clients])

    @app.get(
        f"{CLIENTS_ENDPOINT}/<perimeter>",
        tags=[clients_tag],
        responses={200: ClientsResponse},
    )
    def get_clients_by_perimeter(path: PerimeterPath) -> ClientsResponse:
        """Lister les clients actifs sur un périmètre donné"""
        clients: list[Client] = app.config[ANNUAIRE_CLIENTS_DATA_KEY]
        filtered = [c for c in clients if c.perimeters[path.perimeter]]
        return jsonify([c.model_dump(by_alias=True) for c in filtered])

    @app.get(HEALTH_ENDPOINT, doc_ui=False)
    def health_check() -> tuple[Response, int]:
        return jsonify({"status": "UP", "service": "SAMU Hub Annuaire"}), 200

    # redirects annuaire/api/specs to Swagger UI
    @app.route(SPECS_ENDPOINT)
    def specs_home() -> Response:
        return redirect(f"{SPECS_ENDPOINT}/swagger")


def get_allowed_origins() -> list[str] | None:
    ALLOWED_ORIGINS = os.getenv("ALLOWED_ORIGINS")
    if ALLOWED_ORIGINS:
        return ALLOWED_ORIGINS.split(",")
    else:
        return None


def create_app() -> OpenAPI:
    info = Info(
        title="API Annuaire",
        version="",
        description="Annuaire des clients SAMU Hub joignables, par périmètre.",
    )
    swagger_config = {
        # Avoid configuring an external endpoint to validate the
        # openapi spec generated (used to dispay a status badge).
        "validatorUrl": None
    }
    # Swagger UI : /annuaire/api/specs/swagger  (redirigé depuis /annuaire/api/specs)
    # ReDoc      : /annuaire/api/specs/redoc
    # Spec JSON  : /annuaire/api/specs/openapi.json
    app = OpenAPI(
        __name__,
        info=info,
        doc_prefix=SPECS_ENDPOINT,
        validation_error_status=HTTPStatus.BAD_REQUEST,
        validation_error_model=ErrorResponse,
        validation_error_callback=validation_error_callback,
    )
    app.config["SWAGGER_CONFIG"] = swagger_config
    allowed_origins = get_allowed_origins()
    if allowed_origins:
        CORS(app, origins=allowed_origins)
    register_routes(app)
    clients = load_clients(VALUES_PATH)
    app.config[ANNUAIRE_CLIENTS_DATA_KEY] = build_annuaire_clients(clients)
    return app
