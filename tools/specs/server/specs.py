import logging
import os

from dotenv import load_dotenv
from flask import Flask, jsonify
from flask_cors import CORS

from cache import cache
from config import Config
from routes.auth import auth_bp, init_oauth
from routes.repo import repo_bp

load_dotenv()

logging.basicConfig(
    level=os.getenv("LOG_LEVEL", "INFO"),
    format="%(asctime)s %(levelname)s %(name)s: %(message)s",
)


def create_app():
    app = Flask(__name__)
    app.secret_key = Config.SECRET_KEY
    CORS(app, supports_credentials=True, origins=[Config.CLIENT_URL])
    cache.init_app(app)
    init_oauth(app)
    app.register_blueprint(auth_bp)
    app.register_blueprint(repo_bp)

    @app.get("/health")
    def health():
        return jsonify({"status": "UP", "service": "SAMU Hub specs"}), 200

    return app
