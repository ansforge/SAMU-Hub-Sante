from github import GithubException

from config import Config
from services.github_account import GithubAccount


class ServiceAccount(GithubAccount):
    def _get_token(self):
        return Config.GITHUB_TOKEN

    def get_refs(self) -> dict[str, list[str]]:
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
            self.close()
            raise

    def get_collaborator_permission(self, username: str) -> str | None:
        client = self._get_client()
        try:
            repo = client.get_repo(self.repo_full_name)
            return repo.get_collaborator_permission(username)
        except GithubException:
            return None
