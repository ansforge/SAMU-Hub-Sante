from unittest.mock import patch

from github import GithubException

from specs import create_app


def _client():
    return create_app().test_client()


def test_health():
    res = _client().get("/health")
    assert res.status_code == 200
    assert res.get_json()["status"] == "UP"


@patch("specs.get_schemas")
def test_schemas_returns_missing_token_as_500(mock_get_schemas):
    mock_get_schemas.side_effect = RuntimeError("GITHUB_TOKEN environment variable is not set")

    res = _client().get("/schemas")

    assert res.status_code == 500
    assert "GITHUB_TOKEN" in res.get_json()["error"]


@patch("specs.get_schemas")
def test_schemas_returns_github_error_as_502(mock_get_schemas):
    mock_get_schemas.side_effect = GithubException(404, {"message": "Not Found"}, None)

    res = _client().get("/schemas")

    assert res.status_code == 502
    assert res.get_json()["error"] == "GitHub API error"
