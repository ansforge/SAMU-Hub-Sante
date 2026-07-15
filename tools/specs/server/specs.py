from flask import Flask, jsonify
from flask_cors import CORS

def create_app():
    app = Flask(__name__)
    CORS(app)

    @app.get("/health")
    def health():
        return jsonify({
            "status": "UP",
            "service": "SAMU Hub specs"
        }), 200

    return app