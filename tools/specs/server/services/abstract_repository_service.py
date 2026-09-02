from abc import ABC, abstractmethod

from github import Auth, Github, Repository

from config import Config


class AbstractRepositoryService(ABC):
    def __init__(self):
        self._client: Github | None = None
        self.repo_full_name = f"{Config.REPO_OWNER}/{Config.REPO_NAME}"

    @abstractmethod
    def _get_token(self) -> str:
        pass

    def _get_client(self) -> Github:
        if self._client is None:
            token = self._get_token()
            if not token:
                raise ValueError("Missing GITHUB_TOKEN configuration")
            self._client = Github(auth=Auth.Token(token), timeout=15)
        return self._client

    def _get_repo(self) -> Repository.Repository:
        return self._get_client().get_repo(self.repo_full_name)

    def close(self) -> None:
        if self._client is not None:
            self._client.close()
            self._client = None
