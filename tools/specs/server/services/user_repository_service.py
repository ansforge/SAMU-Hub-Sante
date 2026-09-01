from services.abstract_repository_service import AbstractRepositoryService

REPO_FULL_NAME = "ansforge/SAMU-Hub-Modeles"


class UserRepositoryService(AbstractRepositoryService):
    def __init__(
        self,
        token: str,
    ):
        super().__init__()
        self.token = token

    def _get_token(self) -> str:
        return self.token

    def get_me(self) -> dict:
        client = self._get_client()
        user = client.get_user()
        return {
            "login": user.login,
            "name": user.name,
            "avatar_url": user.avatar_url,
        }
