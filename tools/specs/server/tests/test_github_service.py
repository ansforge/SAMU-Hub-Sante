from unittest.mock import MagicMock, patch

from services.github_service import GithubService


def _mock_ref(name):
    ref = MagicMock()
    ref.name = name
    return ref


def get_refs():
    service = GithubService(token="dummy_token")
    return service.get_refs()


@patch("services.github_service.Github")
def test_get_refs_returns_ref_names(mock_github_cls):
    mock_repo = MagicMock()
    mock_repo.get_branches.return_value = [_mock_ref("main"), _mock_ref("develop")]
    mock_repo.get_tags.return_value = [_mock_ref("v1"), _mock_ref("v2")]
    mock_gh = mock_github_cls.return_value
    mock_gh.get_repo.return_value = mock_repo

    refs = get_refs()

    assert refs == {
        "branches": ["main", "develop"],
        "tags": ["v1", "v2"],
    }
    mock_gh.close.assert_not_called()
