import logging

from authlib.integrations.flask_client import OAuth
from flask import Blueprint, Response, g, jsonify, request
from github import GithubException
from pydantic import ValidationError

from cache import cache
from decorators import auth_required
from routes.schemas import UpdateSchemaPayload
from services.internal_repository_service import InternalRepositoryService

logger = logging.getLogger(__name__)

repo_bp = Blueprint("repo", __name__, url_prefix="/repo")
oauth = OAuth()


@repo_bp.get("/refs")
@cache.cached()
def list_refs() -> Response | tuple[Response, int]:
    service = InternalRepositoryService()
    try:
        refs = service.get_refs()
        return jsonify(refs)
    except GithubException as e:
        logger.warning("list_refs GitHub API error: %s", e)
        return jsonify({"error": "GitHub API error", "detail": str(e)}), 502
    except ValueError as e:
        logger.warning("list_refs config error: %s", e)
        return jsonify({"error": str(e)}), 500
    except Exception:
        logger.exception("list_refs failed")
        return jsonify({"error": "Internal error"}), 500
    finally:
        service.close()


@repo_bp.put("/schema/<schema_id>")
@auth_required
def update_schema(schema_id):
    body = request.get_json(silent=True)
    if not body:
        return jsonify({"error": "Invalid or missing JSON body."}), 400
    try:
        payload = UpdateSchemaPayload.model_validate(body)
    except ValidationError as e:
        return jsonify({"error": e.errors()[0]["msg"]}), 400

    try:
        commit_url = g.github_service.update_schema(
            payload.ref,
            payload.new_branch,
            schema_id,
            payload.data,
            payload.commit_message,
        )
        return jsonify(
            {"status": "success", "schema_id": schema_id, "commit_url": commit_url}
        )
    except GithubException as e:
        logger.warning(
            "update_schema GitHub error for %s@%s: %s", schema_id, payload.ref, e.data
        )
        return jsonify({"error": "Erreur GitHub", "details": e.data}), e.status
    except ValueError as e:
        logger.warning("update_schema rejected for %s: %s", schema_id, e)
        return jsonify({"error": str(e)}), 400
    except Exception:
        logger.exception("update_schema failed")
        return jsonify({"error": "Internal error"}), 500
