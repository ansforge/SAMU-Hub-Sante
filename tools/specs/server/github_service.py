import os
import time
from typing import List
from github import Github, Auth
from models import SchemaReference

SCHEMA_SUFFIX = ".schema.json"
CACHE_TTL_SECONDS = 1800 # 30 mins

_cache: List[SchemaReference] | None = None
_cache_expiry = 0.0


def get_schemas() -> List[SchemaReference]:
    global _cache, _cache_expiry
    if _cache is not None and time.monotonic() < _cache_expiry:
        return _cache

    token = os.getenv("GITHUB_TOKEN")
    if not token:
        raise RuntimeError("GITHUB_TOKEN environment variable is not set")

    auth = Auth.Token(token)
    schemas: List[SchemaReference] = []
    g = Github(auth=auth, timeout=15)
    try:
        repo = g.get_repo("ansforge/SAMU-Hub-Modeles")
        contents = repo.get_contents("src/main/resources/json-schema")
        for item in contents:
            if item.type == "file" and item.name.endswith(SCHEMA_SUFFIX):
                schemas.append(SchemaReference(
                    name=item.name.removesuffix(SCHEMA_SUFFIX),
                    path=item.path,
                    sha=item.sha,
                    url=item.download_url
                ))
    finally:
        g.close()

    _cache = schemas
    _cache_expiry = time.monotonic() + CACHE_TTL_SECONDS
    return _cache
