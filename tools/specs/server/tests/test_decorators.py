from flask import Flask, g

from decorators import auth_required


def _app():
    app = Flask(__name__)

    @app.get("/protected")
    @auth_required
    def protected():
        return {"token": g.github_service.token}

    return app


def test_auth_required_rejects_missing_cookie():
    res = _app().test_client().get("/protected")
    assert res.status_code == 401


def test_auth_required_passes_token_through():
    client = _app().test_client()
    client.set_cookie("gh_token", "abc123")
    res = client.get("/protected")
    assert res.status_code == 200
    assert res.get_json() == {"token": "abc123"}
