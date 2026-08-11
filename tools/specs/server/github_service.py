import os
from typing import List
from github import Github, Auth

REPO_FULL_NAME = "ansforge/SAMU-Hub-Modeles"


class GithubService:
    def __init__(
        self,
        repo_full_name: str = REPO_FULL_NAME,
    ):
        self.repo_full_name = repo_full_name
        self._client: Github | None = None

    def _get_token(self) -> str:
        token = os.getenv("GITHUB_TOKEN")
        if not token:
            raise RuntimeError("GITHUB_TOKEN environment variable is not set")
        return token

    def _get_client(self) -> Github:
        if self._client is None:
            self._client = Github(auth=Auth.Token(self._get_token()), timeout=15)
        return self._client

    def get_refs(self) -> List[str]:
        client = self._get_client()
        try:
            repo = client.get_repo(self.repo_full_name)
            branches = repo.get_branches()
            tags = repo.get_tags()
            return {
                "branches": [branch.name for branch in branches],
                "tags": [tag.name for tag in tags],
            }
        except Exception:
            client.close()
            self._client = None
            raise


_service = GithubService()


def get_refs() -> List[str]:
    return _service.get_refs()
