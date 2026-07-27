from pydantic import BaseModel, Field

class SchemaReference(BaseModel):
    name: str = Field(..., description="The name of the schema (ex 'RC-EDA')")
    url: str = Field(..., description="The github CDN url to schema ('ex https://raw.githubusercontent.com/ansforge/SAMU-Hub-Modeles/main/src/main/resources/json-schema/GEO-REQ.schema.json')")