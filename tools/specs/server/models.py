from pydantic import BaseModel, Field

class SchemaReference(BaseModel):
    name: str = Field(..., description="The name of the schema (ex 'RC-EDA')")
    path: str = Field(..., description="The path of the schema (ex 'src/main/resources/json-schema/GEO-REQ.schema.json')")
    sha: str = Field(..., description="The SHA-1 hash of the file blob (for future comparaison)")
    url: str = Field(..., description="The github CDN url to schema ('ex https://raw.githubusercontent.com/ansforge/SAMU-Hub-Modeles/main/src/main/resources/json-schema/GEO-REQ.schema.json')")