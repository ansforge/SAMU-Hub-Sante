import logging

from github import GithubException

from config import Config
from services.abstract_repository_service import AbstractRepositoryService

logger = logging.getLogger(__name__)


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

    def _create_branch(self, ref: str, base_ref: str) -> str:
        base_ref_obj = self._get_repo().get_git_ref(f"heads/{base_ref}")
        self._get_repo().create_git_ref(
            ref=f"refs/heads/{ref}", sha=base_ref_obj.object.sha
        )
        logger.info("created branch %s from %s", ref, base_ref)
        return ref

    def get_schema(self, ref: str, schema_id: str):
        repo = self._get_repo()
        file_path = f"{Config.SCHEMAS_PATH}/{schema_id}"
        try:
            return repo.get_contents(file_path, ref=ref)
        except GithubException as e:
            logger.warning("get_contents failed for %s@%s: %s", file_path, ref, e.data)
            raise

    def update_schema(
        self,
        ref: str,
        new_branch: str | None,
        schema_id: str,
        schema_data: str,
        commit_message: str,
    ) -> str:
        branch = ref
        if new_branch:
            branch = self._create_branch(new_branch, ref)
        schema = self.get_schema(ref=ref, schema_id=schema_id)
        try:
            commit_message = f"[auto/specs]:{commit_message}"
            res = self._get_repo().update_file(
                path=schema.path,
                message=commit_message,
                content=schema_data,
                sha=schema.sha,
                branch=branch,
            )
        except Exception:
            logger.exception("update_schema failed for %s on %s", schema_id, ref)
            raise

        logger.info("updated schema %s on %s", schema_id, ref)
        return res["commit"].html_url
