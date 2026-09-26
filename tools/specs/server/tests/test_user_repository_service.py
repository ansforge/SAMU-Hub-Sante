from unittest.mock import MagicMock, patch

import pytest
from github import GithubException

from config import Config
from services.user_repository_service import UserRepositoryService


@patch("services.abstract_repository_service.Github")
def test_get_me_returns_profile(mock_github_cls):
    mock_user = MagicMock(login="alice", avatar_url="http://a")
    mock_user.name = "Alice"
    mock_github_cls.return_value.get_user.return_value = mock_user

    result = UserRepositoryService(token="t").get_me()

    assert result == {"login": "alice", "name": "Alice", "avatar_url": "http://a"}


@patch("services.abstract_repository_service.Github")
def test_create_branch_creates_ref_from_base_and_returns_branch_name(mock_github_cls):
    mock_repo = mock_github_cls.return_value.get_repo.return_value
    base_ref_obj = MagicMock()
    base_ref_obj.object.sha = "sha123"
    mock_repo.get_git_ref.return_value = base_ref_obj

    result = UserRepositoryService(token="t")._create_branch("feature", "main")

    mock_repo.get_git_ref.assert_called_once_with("heads/main")
    mock_repo.create_git_ref.assert_called_once_with(
        ref="refs/heads/feature", sha="sha123"
    )
    assert result == "feature"


@patch("services.abstract_repository_service.Github")
def test_get_schema_returns_contents(mock_github_cls):
    mock_repo = mock_github_cls.return_value.get_repo.return_value
    file_path = f"{Config.SCHEMAS_PATH}/s.json"
    mock_repo.get_contents.return_value = MagicMock(path=file_path, sha="abc")

    result = UserRepositoryService(token="t").get_schema("feature", "s.json")

    mock_repo.get_contents.assert_called_once_with(file_path, ref="feature")
    assert result.sha == "abc"


@patch("services.abstract_repository_service.Github")
def test_get_schema_propagates_404(mock_github_cls):
    mock_repo = mock_github_cls.return_value.get_repo.return_value
    mock_repo.get_contents.side_effect = GithubException(404, {"message": "Not Found"})

    with pytest.raises(GithubException):
        UserRepositoryService(token="t").get_schema("feature", "missing.json")


@patch("services.abstract_repository_service.Github")
def test_update_schema_without_new_branch_commits_directly_on_ref(mock_github_cls):
    mock_repo = mock_github_cls.return_value.get_repo.return_value
    file_path = f"{Config.SCHEMAS_PATH}/s.json"
    mock_schema = MagicMock(path=file_path, sha="abc")
    mock_repo.get_contents.return_value = mock_schema
    mock_repo.update_file.return_value = {"commit": MagicMock(html_url="http://commit")}

    url = UserRepositoryService(token="t").update_schema(
        "feature", None, "s.json", "{}", "msg"
    )

    assert url == "http://commit"
    mock_repo.create_git_ref.assert_not_called()
    mock_repo.update_file.assert_called_once_with(
        path=file_path,
        message="[auto/specs]:msg",
        content="{}",
        sha="abc",
        branch="feature",
    )


@patch("services.abstract_repository_service.Github")
def test_update_schema_with_new_branch_creates_branch_and_commits_on_it(
    mock_github_cls,
):
    mock_repo = mock_github_cls.return_value.get_repo.return_value
    base_ref_obj = MagicMock()
    base_ref_obj.object.sha = "sha123"
    mock_repo.get_git_ref.return_value = base_ref_obj
    file_path = f"{Config.SCHEMAS_PATH}/s.json"
    mock_schema = MagicMock(path=file_path, sha="abc")
    mock_repo.get_contents.return_value = mock_schema
    mock_repo.update_file.return_value = {"commit": MagicMock(html_url="http://commit")}

    url = UserRepositoryService(token="t").update_schema(
        "main", "feature", "s.json", "{}", "msg"
    )

    assert url == "http://commit"
    mock_repo.get_git_ref.assert_called_once_with("heads/main")
    mock_repo.create_git_ref.assert_called_once_with(
        ref="refs/heads/feature", sha="sha123"
    )
    mock_repo.get_contents.assert_called_once_with(file_path, ref="main")
    mock_repo.update_file.assert_called_once_with(
        path=file_path,
        message="[auto/specs]:msg",
        content="{}",
        sha="abc",
        branch="feature",
    )


@patch("services.abstract_repository_service.Github")
def test_update_schema_propagates_404_from_get_contents(mock_github_cls):
    mock_repo = mock_github_cls.return_value.get_repo.return_value
    mock_repo.get_contents.side_effect = GithubException(404, {"message": "Not Found"})

    with pytest.raises(GithubException):
        UserRepositoryService(token="t").update_schema(
            "feature", None, "missing.json", "{}", "msg"
        )


@patch("services.abstract_repository_service.Github")
def test_update_schema_propagates_error_from_update_file(mock_github_cls):
    mock_repo = mock_github_cls.return_value.get_repo.return_value
    mock_repo.get_contents.return_value = MagicMock(path="p", sha="abc")
    mock_repo.update_file.side_effect = GithubException(409, {"message": "Conflict"})

    with pytest.raises(GithubException):
        UserRepositoryService(token="t").update_schema(
            "feature", None, "s.json", "{}", "msg"
        )
