from unittest.mock import MagicMock, patch

from config import Config
from services.internal_repository_service import InternalRepositoryService


def _mock_ref(name):
    ref = MagicMock()
    ref.name = name
    return ref


@patch.object(Config, "GITHUB_TOKEN", "dummy_token")
@patch("services.abstract_repository_service.Github")
def test_get_refs_returns_ref_names(mock_github_cls):
    mock_repo = MagicMock()
    mock_repo.get_branches.return_value = [_mock_ref("main"), _mock_ref("develop")]
    mock_repo.get_tags.return_value = [_mock_ref("v1"), _mock_ref("v2")]
    mock_gh = mock_github_cls.return_value
    mock_gh.get_repo.return_value = mock_repo

    refs = InternalRepositoryService().get_refs()

    assert refs == {
        "branches": ["main", "develop"],
        "tags": ["v1", "v2"],
    }
    mock_gh.close.assert_not_called()
