from pydantic import BaseModel, RootModel
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


class Client(BaseModel):
    """Un client tel qu'il est exposé par l'API."""

    client_id: str
    client_name: str
    client_type: str
    perimeters: dict[Perimeter, bool]


class ClientsResponse(RootModel[list[Client]]):
    """Réponse en tableau nu : documente `[Client, ...]` à la racine."""


class ErrorResponse(BaseModel):
    error: str
    valid_perimeters: list[str]


class PerimeterPath(BaseModel):
    perimeter: Perimeter
