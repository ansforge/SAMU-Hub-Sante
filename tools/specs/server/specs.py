import os

from dotenv import load_dotenv
from flask import Flask, Response, jsonify
from flask_caching import Cache
from flask_cors import CORS
from github import GithubException

from github_service import GithubService

load_dotenv()

cache = Cache(config={"CACHE_TYPE": "SimpleCache", "CACHE_DEFAULT_TIMEOUT": 300})


def create_app():
    app = Flask(__name__)
    CORS(app)
    cache.init_app(app)

    @app.get("/health")
    def health():
        return jsonify({"status": "UP", "service": "SAMU Hub specs"}), 200

    @app.get("/refs")
    @cache.cached()
    def list_refs() -> Response | tuple[Response, int]:
        token = os.getenv("GITHUB_TOKEN")
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
