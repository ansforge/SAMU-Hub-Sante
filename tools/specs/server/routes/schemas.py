import json

from pydantic import BaseModel, field_validator


class UpdateSchemaPayload(BaseModel):
    ref: str
    data: str
    commit_message: str
    new_branch: str | None = None

    @field_validator("ref", "data", "commit_message")
    @classmethod
    def not_blank(cls, v: str) -> str:
        if not v:
            raise ValueError("must not be empty")
        return v

    @field_validator("data")
    @classmethod
    def is_json(cls, v: str) -> str:
        try:
            json.loads(v)
        except ValueError:
            raise ValueError("must be a valid JSON-encoded string") from None
        return v
