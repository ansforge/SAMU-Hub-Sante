import pytest
from unittest.mock import patch, MagicMock

import github_service
from github_service import get_schemas, get_schema_content, SchemaNotFoundError


@pytest.fixture(autouse=True)
def reset_cache():
    github_service._cache = None
    github_service._cache_expiry = 0.0


def _mock_item(name, type_="file"):
    item = MagicMock()
    item.type = type_
    item.name = name
    item.path = f"src/main/resources/json-schema/{name}"
    item.sha = "abc123"
    item.download_url = f"https://raw.githubusercontent.com/ansforge/SAMU-Hub-Modeles/main/{name}"
    return item


@patch("github_service.Github")
def test_get_schemas_filters_and_maps_schema_files(mock_github_cls, monkeypatch):
    monkeypatch.setenv("GITHUB_TOKEN", "fake-token")
    mock_repo = MagicMock()
    mock_repo.get_contents.return_value = [
        _mock_item("GEO-REQ.schema.json"),
        _mock_item("README.md"),
        _mock_item("subdir", type_="dir"),
    ]
    mock_gh = mock_github_cls.return_value
    mock_gh.get_repo.return_value = mock_repo

    schemas = get_schemas()

    assert len(schemas) == 1
    schema = schemas[0]
    assert schema.name == "GEO-REQ"
    assert schema.path == "src/main/resources/json-schema/GEO-REQ.schema.json"
    assert schema.sha == "abc123"
    assert schema.url == "https://raw.githubusercontent.com/ansforge/SAMU-Hub-Modeles/main/GEO-REQ.schema.json"
    mock_gh.close.assert_called_once()


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


@patch("github_service.Github")
def test_get_schema_content_returns_decoded_file(mock_github_cls, monkeypatch):
    monkeypatch.setenv("GITHUB_TOKEN", "fake-token")
    mock_repo = MagicMock()
    content_file = MagicMock()
    content_file.decoded_content = b'{"title": "GEO-REQ"}'
    mock_repo.get_contents.side_effect = [
        [_mock_item("GEO-REQ.schema.json")],
        content_file,
    ]
    mock_gh = mock_github_cls.return_value
    mock_gh.get_repo.return_value = mock_repo

    content = get_schema_content("GEO-REQ")

    assert content == '{"title": "GEO-REQ"}'
    mock_repo.get_contents.assert_called_with("src/main/resources/json-schema/GEO-REQ.schema.json")
    assert mock_gh.close.call_count == 2


@patch("github_service.Github")
def test_get_schema_content_raises_when_schema_unknown(mock_github_cls, monkeypatch):
    monkeypatch.setenv("GITHUB_TOKEN", "fake-token")
    mock_repo = MagicMock()
    mock_repo.get_contents.return_value = [_mock_item("GEO-REQ.schema.json")]
    mock_gh = mock_github_cls.return_value
    mock_gh.get_repo.return_value = mock_repo

    with pytest.raises(SchemaNotFoundError):
        get_schema_content("unknown")


def test_get_schema_content_raises_when_token_missing(monkeypatch):
    monkeypatch.delenv("GITHUB_TOKEN", raising=False)

    with pytest.raises(RuntimeError, match="GITHUB_TOKEN"):
        get_schema_content("GEO-REQ")
