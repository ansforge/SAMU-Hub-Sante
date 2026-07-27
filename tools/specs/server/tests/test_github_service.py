import json

import pytest
from unittest.mock import patch, MagicMock

import github_service
from github_service import get_schemas


@pytest.fixture(autouse=True)
def reset_cache():
    github_service._service = github_service.GithubSchemaService()


def _mock_messages_list(items):
    content = MagicMock()
    content.decoded_content = json.dumps(items).encode("utf-8")
    return content


@patch("github_service.Github")
def test_get_schemas_filters_and_maps_schema_files(mock_github_cls, monkeypatch):
    monkeypatch.setenv("GITHUB_TOKEN", "fake-token")
    mock_repo = MagicMock()
    mock_repo.get_contents.return_value = _mock_messages_list(
        [{"label": "GEO-REQ", "schemaName": "GEO-REQ.schema.json"}]
    )
    mock_gh = mock_github_cls.return_value
    mock_gh.get_repo.return_value = mock_repo

    schemas = get_schemas()

    assert len(schemas) == 1
    schema = schemas[0]
    assert schema.name == "GEO-REQ"
    assert schema.url == "https://raw.githubusercontent.com/ansforge/SAMU-Hub-Modeles/main/src/main/resources/json-schema/GEO-REQ.schema.json"
    mock_gh.close.assert_not_called()


@patch("github_service.Github")
def test_get_schemas_uses_custom_ref(mock_github_cls, monkeypatch):
    monkeypatch.setenv("GITHUB_TOKEN", "fake-token")
    mock_repo = MagicMock()
    mock_repo.get_contents.return_value = _mock_messages_list(
        [{"label": "GEO-REQ", "schemaName": "GEO-REQ.schema.json"}]
    )
    mock_gh = mock_github_cls.return_value
    mock_gh.get_repo.return_value = mock_repo

    github_service._service = github_service.GithubSchemaService(ref="develop")

    schemas = get_schemas()

    mock_repo.get_contents.assert_called_once_with(
        "src/main/resources/sample/examples/messagesList.json",
        ref="develop",
    )
    assert schemas[0].url == "https://raw.githubusercontent.com/ansforge/SAMU-Hub-Modeles/develop/src/main/resources/json-schema/GEO-REQ.schema.json"


def test_get_schemas_raises_when_token_missing(monkeypatch):
    monkeypatch.delenv("GITHUB_TOKEN", raising=False)

    with pytest.raises(RuntimeError, match="GITHUB_TOKEN"):
        get_schemas()


@patch("github_service.Github")
def test_get_schemas_closes_client_on_error(mock_github_cls, monkeypatch):
    monkeypatch.setenv("GITHUB_TOKEN", "fake-token")
    mock_gh = mock_github_cls.return_value
    mock_gh.get_repo.side_effect = RuntimeError("boom")

    try:
        get_schemas()
    except RuntimeError:
        pass

    mock_gh.close.assert_called_once()


@patch("github_service.requests.get")
@patch("github_service.Github")
def test_get_schema_content_returns_decoded_file(mock_github_cls, mock_requests_get, monkeypatch):
    monkeypatch.setenv("GITHUB_TOKEN", "fake-token")
    mock_repo = MagicMock()
    mock_repo.get_contents.return_value = _mock_messages_list(
        [{"label": "GEO-REQ", "schemaName": "GEO-REQ.schema.json"}]
    )
    mock_gh = mock_github_cls.return_value
    mock_gh.get_repo.return_value = mock_repo

    schemas = get_schemas(ref="develop")

    assert content == '{"title": "GEO-REQ"}'
    mock_requests_get.assert_called_once_with(
        "https://raw.githubusercontent.com/ansforge/SAMU-Hub-Modeles/main/src/main/resources/json-schema/GEO-REQ.schema.json",
        headers={"Authorization": "token fake-token"},
        timeout=15,
    )
    assert schemas[0].url == "https://raw.githubusercontent.com/ansforge/SAMU-Hub-Modeles/develop/src/main/resources/json-schema/GEO-REQ.schema.json"


@patch("github_service.Github")
def test_get_schemas_caches_per_ref(mock_github_cls, monkeypatch):
    monkeypatch.setenv("GITHUB_TOKEN", "fake-token")
    mock_repo = MagicMock()
    mock_repo.get_contents.return_value = _mock_messages_list(
        [{"label": "GEO-REQ", "schemaName": "GEO-REQ.schema.json"}]
    )
    mock_gh = mock_github_cls.return_value
    mock_gh.get_repo.return_value = mock_repo

    get_schemas(ref="main")
    get_schemas(ref="develop")
    get_schemas(ref="main")

    assert mock_repo.get_contents.call_count == 2
