from github import Auth, Github

REPO_FULL_NAME = "ansforge/SAMU-Hub-Modeles"


class GithubService:
    def __init__(
        self,
        token: str,
        repo_full_name: str = REPO_FULL_NAME,
    ):
        self.repo_full_name = repo_full_name
        self.token = token
        self._client: Github | None = None

    def _get_token(self) -> str:
        return self.token

    def _get_client(self) -> Github:
        if self._client is None:
            self._client = Github(auth=Auth.Token(self._get_token()), timeout=15)
        return self._client

    def close(self) -> None:
        if self._client is not None:
            self._client.close()
            self._client = None

    def get_refs(self) -> list[str]:
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
