from pydantic import BaseModel, RootModel, ConfigDict, Field


class Perimeters(BaseModel):
    model_config = ConfigDict(
        populate_by_name=True,
        extra="ignore",
    )

    lrm: bool = Field(False, alias="15-15")
    cap: bool = Field(False, alias="15-cap")
    portail: bool = Field(False, alias="15-portail")
    cnr114: bool = Field(False, alias="15-cnr114")
    cisu: bool = Field(False, alias="15-nexsis")
    smur: bool = Field(False, alias="15-smur")
    gps: bool = Field(False, alias="15-gps")

    # allows to do : perimeter.get_by_alias("15-15") => True
    def get_by_alias(self, alias: str) -> bool:
        for name, field in self.__class__.model_fields.items():
            if field.alias == alias:
                return getattr(self, name)

        raise KeyError(alias)


class Client(BaseModel):
    """Un client tel qu'il est exposé par l'API."""

    client_id: str
    client_name: str
    client_type: str
    perimeters: Perimeters


class ClientsResponse(RootModel[list[Client]]):
    """Réponse en tableau nu : documente `[Client, ...]` à la racine."""


class ErrorResponse(BaseModel):
    error: str
    valid_perimeters: list[str]


class PerimeterPath(BaseModel):
    perimeter: str
