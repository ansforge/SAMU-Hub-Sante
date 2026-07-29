from dotenv import load_dotenv
from flask import Flask, jsonify, request, Response
from flask_cors import CORS
from github import GithubException

from github_service import get_schemas

load_dotenv()

def create_app():
    app = Flask(__name__)
    CORS(app)

    @app.get("/health")
    def health():
        return jsonify({
            "status": "UP",
            "service": "SAMU Hub specs"
        }), 200

    @app.get("/schemas")
    def list_schemas() -> Response:
        try:
            schemas = get_schemas(ref=request.args.get("ref"))
        except RuntimeError as e:
            return jsonify({"error": str(e)}), 500
        except GithubException as e:
            return jsonify({"error": "GitHub API error", "detail": str(e)}), 502
        return jsonify([s.model_dump() for s in schemas])

    return app