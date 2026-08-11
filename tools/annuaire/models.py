from pydantic import BaseModel, ConfigDict, Field, RootModel
from enum import StrEnum


class Perimeter(StrEnum):
    LRM = "15-15"
    CAP = "15-cap"
    PORTAIL = "15-portail"
    CNR114 = "15-cnr114"
    CISU = "15-nexsis"
    SMUR = "15-smur"
    GPS = "15-gps"


PERIMETER_TO_ANNUAIRE_KEY_MAP = {
    Perimeter.LRM: "lrm",
    Perimeter.CAP: "cap",
    Perimeter.PORTAIL: "portail",
    Perimeter.CNR114: "cnr114",
    Perimeter.CISU: "cisu",
    Perimeter.SMUR: "smur",
    Perimeter.GPS: "gps",
}


class Perimeters(BaseModel):
    model_config = ConfigDict(populate_by_name=True)
    lrm: bool = Field(False, alias=Perimeter.LRM)
    cap: bool = Field(False, alias=Perimeter.CAP)
    portail: bool = Field(False, alias=Perimeter.PORTAIL)
    cnr114: bool = Field(False, alias=Perimeter.CNR114)
    cisu: bool = Field(False, alias=Perimeter.CISU)
    smur: bool = Field(False, alias=Perimeter.SMUR)
    gps: bool = Field(False, alias=Perimeter.GPS)

    def __getitem__(self, p: Perimeter) -> bool:
        return getattr(self, PERIMETER_TO_ANNUAIRE_KEY_MAP[p])


class Client(BaseModel):
    """Un client tel qu'il est exposé par l'API."""

    client_id: str
    client_name: str
    client_type: str
    perimeters: Perimeters


class ClientsResponse(RootModel[list[Client]]):
    """Réponse en tableau nu : documente `[Client, ...]` à la racine."""


class ErrorResponse(BaseModel):
    error: str = Field(examples=["Invalid perimeter"])
    valid_perimeters: list[Perimeter]
    model_config = {
        "json_schema_extra": {
            "example": {
                "error": "Invalid perimeter",
                "valid_perimeters": list(Perimeter),
            }
        }
    }


class PerimeterPath(BaseModel):
    perimeter: Perimeter
