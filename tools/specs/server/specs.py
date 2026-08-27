import os

from dotenv import load_dotenv
from flask import Flask, Response, jsonify, request
from flask_caching import Cache
from flask_cors import CORS
from github import GithubException

from config import Config
from routes.auth import auth_bp, init_oauth
from services.github_service import GithubService

load_dotenv()

cache = Cache(config={"CACHE_TYPE": "SimpleCache", "CACHE_DEFAULT_TIMEOUT": 300})


def create_app():
    app = Flask(__name__)
    app.secret_key = Config.SECRET_KEY
    CORS(app, supports_credentials=True, origins=[Config.CLIENT_URL])
    cache.init_app(app)
    init_oauth(app)
    app.register_blueprint(auth_bp)

    @app.get("/health")
    def health():
        return jsonify({"status": "UP", "service": "SAMU Hub specs"}), 200

    @app.get("/refs")
    @cache.cached()
    def list_refs() -> Response | tuple[Response, int]:
        token = request.cookies.get("gh_token") or os.getenv("GITHUB_TOKEN")
        if not token:
            return jsonify(
                {"error": "GITHUB_TOKEN environment variable is not set"}
            ), 500
        service = GithubService(token=token)
        try:
            refs = service.get_refs()
            return jsonify(refs)
        except GithubException as e:
            return jsonify({"error": "GitHub API error", "detail": str(e)}), 502
        finally:
            service.close()

    return app
