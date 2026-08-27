import os
from unittest.mock import patch

from github import GithubException

from specs import create_app


def _client():
    return create_app().test_client()


def test_health():
    res = _client().get("/health")
    assert res.status_code == 200
    assert res.get_json()["status"] == "UP"


@patch("specs.GithubService")
@patch.dict(os.environ, {"GITHUB_TOKEN": "dummy_token"})
def test_refs_returns_list(mock_service_cls):
    mock_service = mock_service_cls.return_value
    mock_service.get_refs.return_value = {
        "branches": ["main", "develop"],
        "tags": ["v1", "v2"],
    }

    res = _client().get("/refs")

    assert res.status_code == 200
    assert res.get_json() == {
        "branches": ["main", "develop"],
        "tags": ["v1", "v2"],
    }
    mock_service.close.assert_called_once()


@patch.dict(os.environ, {}, clear=True)
def test_refs_returns_missing_token_as_500():
    res = _client().get("/refs")

    assert res.status_code == 500
    assert "GITHUB_TOKEN" in res.get_json()["error"]


@patch("specs.GithubService")
@patch.dict(os.environ, {}, clear=True)
def test_refs_uses_cookie_token_over_env(mock_service_cls):
    mock_service = mock_service_cls.return_value
    mock_service.get_refs.return_value = {"branches": [], "tags": []}

    client = _client()
    client.set_cookie("gh_token", "cookie_token")
    res = client.get("/refs")

    assert res.status_code == 200
    mock_service_cls.assert_called_once_with(token="cookie_token")


@patch("specs.GithubService")
@patch.dict(os.environ, {"GITHUB_TOKEN": "dummy_token"})
def test_refs_returns_github_error_as_502(mock_service_cls):
    mock_service = mock_service_cls.return_value
    mock_service.get_refs.side_effect = GithubException(
        404, {"message": "Not Found"}, None
    )

    res = _client().get("/refs")

    assert res.status_code == 502
    assert res.get_json()["error"] == "GitHub API error"
    mock_service.close.assert_called_once()
