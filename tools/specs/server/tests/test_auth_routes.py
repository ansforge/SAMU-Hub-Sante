from unittest.mock import patch

import pytest

from routes.auth import oauth
from specs import create_app


@pytest.fixture
def client():
    app = create_app()
    with app.app_context():
        yield app.test_client()


def test_login_redirects_to_github(client):
    with patch.object(
        oauth.github, "authorize_redirect", return_value="redirected"
    ) as mock_redirect:
        res = client.get("/auth/github/login")
    assert res.status_code == 200
    mock_redirect.assert_called_once()


def test_callback_success_logs_in(client):
    with (
        patch.object(
            oauth.github,
            "authorize_access_token",
            return_value={"access_token": "gh_token_value"},
        ),
        patch("routes.auth.auth_service") as mock_auth_service,
    ):
        mock_auth_service.verify_collaborator_permissions.return_value = True
        mock_auth_service.create_login_response.return_value = "logged_in"
        res = client.get("/auth/github/callback")

    assert res.status_code == 200
    mock_auth_service.create_login_response.assert_called_once_with("gh_token_value")


def test_callback_forbidden_without_write_access(client):
    with (
        patch.object(
            oauth.github,
            "authorize_access_token",
            return_value={"access_token": "gh_token_value"},
        ),
        patch("routes.auth.auth_service") as mock_auth_service,
    ):
        mock_auth_service.verify_collaborator_permissions.return_value = False
        res = client.get("/auth/github/callback")

    assert res.status_code == 302
    assert "forbidden_no_write_access" in res.headers["Location"]


def test_me_rejects_missing_cookie(client):
    res = client.get("/auth/me")
    assert res.status_code == 401


def test_me_returns_user_when_authenticated(client):
    client.set_cookie("gh_token", "abc123")
    with patch("decorators.GithubService") as mock_service_cls:
        mock_service_cls.return_value.get_me.return_value = {"login": "alice"}
        res = client.get("/auth/me")

    assert res.status_code == 200
    assert res.get_json() == {"authenticated": True, "user": {"login": "alice"}}


def test_logout_clears_cookie(client):
    res = client.post("/auth/logout")
    assert res.status_code == 200
    assert res.get_json() == {"status": "logged_out"}
