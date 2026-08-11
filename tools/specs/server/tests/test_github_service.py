import pytest
from unittest.mock import patch, MagicMock

import github_service
from github_service import get_refs


@pytest.fixture(autouse=True)
def reset_cache():
    github_service._service = github_service.GithubService()


def _mock_ref(name):
    ref = MagicMock()
    ref.name = name
    return ref


@patch("github_service.Github")
def test_get_refs_returns_ref_names(mock_github_cls, monkeypatch):
    monkeypatch.setenv("GITHUB_TOKEN", "fake-token")
    mock_repo = MagicMock()
    mock_repo.get_branches.return_value = [_mock_ref("main"), _mock_ref("develop")]
    mock_repo.get_tags.return_value = [_mock_ref("v1"), _mock_ref("v2")]
    mock_gh = mock_github_cls.return_value
    mock_gh.get_repo.return_value = mock_repo

    refs = get_refs()

    assert refs == {
        "branches": ["main","develop"],
        "tags": ["v1","v2"],
    }
    mock_gh.close.assert_not_called()


def test_get_refs_raises_when_token_missing(monkeypatch):
    monkeypatch.delenv("GITHUB_TOKEN", raising=False)

    with pytest.raises(RuntimeError, match="GITHUB_TOKEN"):
        get_refs()


@patch("github_service.Github")
def test_get_refs_closes_client_on_error(mock_github_cls, monkeypatch):
    monkeypatch.setenv("GITHUB_TOKEN", "fake-token")
    mock_gh = mock_github_cls.return_value
    mock_gh.get_repo.side_effect = RuntimeError("boom")

    try:
        get_refs()
    except RuntimeError:
        pass

    mock_gh.close.assert_called_once()
