from github import GithubException

from config import Config
from services.abstract_repository_service import AbstractRepositoryService


class InternalRepositoryService(AbstractRepositoryService):
    def _get_token(self):
        return Config.GITHUB_TOKEN

    def get_refs(self) -> dict[str, list[str]]:
        repo = self._get_repo()
        try:
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
        repo = self._get_repo()
        try:
            return repo.get_collaborator_permission(username)
        except GithubException:
            return None
