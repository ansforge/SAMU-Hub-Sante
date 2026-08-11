from dotenv import load_dotenv
from flask import Flask, jsonify, Response
from flask_caching import Cache
from flask_cors import CORS
from github import GithubException

from github_service import get_refs

load_dotenv()

cache = Cache(config={"CACHE_TYPE": "SimpleCache", "CACHE_DEFAULT_TIMEOUT": 300})

def create_app():
    app = Flask(__name__)
    CORS(app)
    cache.init_app(app)

    @app.get("/health")
    def health():
        return jsonify({
            "status": "UP",
            "service": "SAMU Hub specs"
        }), 200

    @app.get("/refs")
    @cache.cached()
    def list_refs() -> Response:
        try:
            branches = get_refs()
        except RuntimeError as e:
            return jsonify({"error": str(e)}), 500
        except GithubException as e:
            return jsonify({"error": "GitHub API error", "detail": str(e)}), 502
        return jsonify(branches)

    return app