from unittest.mock import patch

import pytest
from github import GithubException

from cache import cache
from specs import create_app


@pytest.fixture
def client():
    app = create_app()
    with app.app_context():
        cache.clear()
        yield app.test_client()
        cache.clear()


@patch("routes.repo.InternalRepositoryService")
def test_list_refs_returns_refs(mock_service_cls, client):
    mock_service_cls.return_value.get_refs.return_value = {
        "branches": ["main"],
        "tags": ["v1"],
    }

    res = client.get("/repo/refs")

    assert res.status_code == 200
    assert res.get_json() == {"branches": ["main"], "tags": ["v1"]}
    mock_service_cls.return_value.close.assert_called_once()


@patch("routes.repo.InternalRepositoryService")
def test_list_refs_missing_token_returns_500(mock_service_cls, client):
    mock_service_cls.return_value.get_refs.side_effect = ValueError(
        "Missing GITHUB_TOKEN configuration"
    )

    res = client.get("/repo/refs")

    assert res.status_code == 500
    assert res.get_json() == {"error": "Missing GITHUB_TOKEN configuration"}


@patch("routes.repo.InternalRepositoryService")
def test_list_refs_github_error_returns_502(mock_service_cls, client):
    mock_service_cls.return_value.get_refs.side_effect = GithubException(
        503, {"message": "Service unavailable"}
    )

    res = client.get("/repo/refs")

    assert res.status_code == 502


@patch("routes.repo.InternalRepositoryService")
def test_list_refs_unexpected_error_returns_500(mock_service_cls, client):
    mock_service_cls.return_value.get_refs.side_effect = RuntimeError("boom")

    res = client.get("/repo/refs")

    assert res.status_code == 500
    assert res.get_json() == {"error": "Internal error"}


def test_update_schema_rejects_missing_cookie(client):
    res = client.put("/repo/schema/s.json", json={"data": "{}"})
    assert res.status_code == 401


def test_update_schema_rejects_missing_body(client):
    client.set_cookie("gh_token", "abc")
    res = client.put("/repo/schema/s.json")
    assert res.status_code == 400


def test_update_schema_rejects_missing_fields(client):
    client.set_cookie("gh_token", "abc")
    res = client.put("/repo/schema/s.json", json={"data": "{}"})
    assert res.status_code == 400
    assert "required" in res.get_json()["error"]


def test_update_schema_rejects_non_string_data(client):
    client.set_cookie("gh_token", "abc")
    res = client.put(
        "/repo/schema/s.json",
        json={
            "data": {"not": "a string"},
            "ref": "feature",
            "commit_message": "m",
        },
    )
    assert res.status_code == 400


def test_update_schema_rejects_invalid_json_string(client):
    client.set_cookie("gh_token", "abc")
    res = client.put(
        "/repo/schema/s.json",
        json={
            "data": "{not json}",
            "ref": "feature",
            "commit_message": "m",
        },
    )
    assert res.status_code == 400


def test_update_schema_success_without_new_branch(client):
    client.set_cookie("gh_token", "abc")
    with patch("decorators.UserRepositoryService") as mock_service_cls:
        mock_service_cls.return_value.update_schema.return_value = "http://commit"
        res = client.put(
            "/repo/schema/s.json",
            json={
                "data": "{}",
                "ref": "feature",
                "commit_message": "m",
            },
        )

    assert res.status_code == 200
    assert res.get_json() == {
        "status": "success",
        "schema_id": "s.json",
        "commit_url": "http://commit",
    }
    mock_service_cls.return_value.update_schema.assert_called_once_with(
        "feature", None, "s.json", "{}", "m"
    )


def test_update_schema_success_with_new_branch(client):
    client.set_cookie("gh_token", "abc")
    with patch("decorators.UserRepositoryService") as mock_service_cls:
        mock_service_cls.return_value.update_schema.return_value = "http://commit"
        res = client.put(
            "/repo/schema/s.json",
            json={
                "data": "{}",
                "ref": "main",
                "new_branch": "feature",
                "commit_message": "m",
            },
        )

    assert res.status_code == 200
    mock_service_cls.return_value.update_schema.assert_called_once_with(
        "main", "feature", "s.json", "{}", "m"
    )


def test_update_schema_github_error_returns_github_status(client):
    client.set_cookie("gh_token", "abc")
    with patch("decorators.UserRepositoryService") as mock_service_cls:
        mock_service_cls.return_value.update_schema.side_effect = GithubException(
            404, {"message": "Not Found"}
        )
        res = client.put(
            "/repo/schema/s.json",
            json={
                "data": "{}",
                "ref": "feature",
                "commit_message": "m",
            },
        )

    assert res.status_code == 404


def test_update_schema_value_error_returns_400(client):
    client.set_cookie("gh_token", "abc")
    with patch("decorators.UserRepositoryService") as mock_service_cls:
        mock_service_cls.return_value.update_schema.side_effect = ValueError(
            "some validation error."
        )
        res = client.put(
            "/repo/schema/s.json",
            json={
                "data": "{}",
                "ref": "feature",
                "commit_message": "m",
            },
        )

    assert res.status_code == 400
    assert res.get_json() == {"error": "some validation error."}


def test_update_schema_unexpected_error_returns_500(client):
    client.set_cookie("gh_token", "abc")
    with patch("decorators.UserRepositoryService") as mock_service_cls:
        mock_service_cls.return_value.update_schema.side_effect = RuntimeError("boom")
        res = client.put(
            "/repo/schema/s.json",
            json={
                "data": "{}",
                "ref": "feature",
                "commit_message": "m",
            },
        )

    assert res.status_code == 500
    assert res.get_json() == {"error": "Internal error"}
