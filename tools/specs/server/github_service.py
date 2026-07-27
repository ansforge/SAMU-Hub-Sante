import json
import os
from functools import lru_cache
from typing import List
from github import Github, Auth
from models import SchemaReference

REPO_FULL_NAME = "ansforge/SAMU-Hub-Modeles"
DEFAULT_REF = "main"


class GithubSchemaService:
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

    def _construct_schema_path(self, name: str, ref: str) -> str:
        return f"https://raw.githubusercontent.com/{self.repo_full_name}/{ref}/src/main/resources/json-schema/{name}"

    @lru_cache(maxsize=8)
    def get_schemas(self, ref: str) -> List[SchemaReference]:
        client = self._get_client()
        try:
            repo = client.get_repo(self.repo_full_name)
            messages_list_raw = repo.get_contents(
                "src/main/resources/sample/examples/messagesList.json",
                ref=ref,
            )
        except Exception:
            client.close()
            self._client = None
            raise

        messages_list = json.loads(messages_list_raw.decoded_content.decode("utf-8"))

        return [
            SchemaReference(
                name=item.get("label"),
                url=self._construct_schema_path(item.get("schemaName"), ref),
            )
            for item in messages_list
        ]


_service = GithubSchemaService()


def get_schemas(ref: str | None = None) -> List[SchemaReference]:
    return _service.get_schemas(ref or os.getenv("SCHEMA_REF", DEFAULT_REF))
