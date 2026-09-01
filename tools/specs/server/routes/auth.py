import logging

from authlib.integrations.flask_client import OAuth
from flask import Blueprint, g, jsonify, redirect
from github import GithubException

from config import Config
from decorators import auth_required
from services.auth_service import auth_service

logger = logging.getLogger(__name__)

auth_bp = Blueprint("auth", __name__, url_prefix="/auth")
oauth = OAuth()


def init_oauth(app):
    oauth.init_app(app)
    oauth.register(
        name="github",
        client_id=Config.GITHUB_CLIENT_ID,
        client_secret=Config.GITHUB_CLIENT_SECRET,
        access_token_url="https://github.com/login/oauth/access_token",
        authorize_url="https://github.com/login/oauth/authorize",
        api_base_url="https://api.github.com/",
        client_kwargs={"scope": "repo"},
    )


@auth_bp.get("/github/login")
def login():
    redirect_uri = f"{Config.BACKEND_URL}/auth/github/callback"
    return oauth.github.authorize_redirect(redirect_uri)


@auth_bp.get("/github/callback")
def callback():
    try:
        token_data = oauth.github.authorize_access_token()
        access_token = token_data.get("access_token")

        if not access_token:
            return redirect(f"{Config.CLIENT_URL}?error=no_token")

        if not auth_service.verify_collaborator_permissions(access_token):
            return redirect(f"{Config.CLIENT_URL}?error=forbidden_no_write_access")

        return auth_service.create_login_response(access_token)

    except Exception:
        logger.exception("GitHub OAuth callback failed")
        return redirect(f"{Config.CLIENT_URL}?error=auth_failed")


@auth_bp.get("/me")
@auth_required
def get_me():
    try:
        user_info = g.github_service.get_me()
        return jsonify({"authenticated": True, "user": user_info})
    except GithubException as e:
        return jsonify({"authenticated": False, "error": e.data}), 401


@auth_bp.post("/logout")
def logout():
    return auth_service.create_logout_response()
