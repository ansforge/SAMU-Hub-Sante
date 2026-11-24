from enum import Enum
from typing import Iterable


class Status(Enum):
    UP = "UP"
    DOWN = "DOWN"
    DEGRADED = "DEGRADED"
    OK = "ok"
    UNKNOWN = "UNKNOWN"


def aggregate_statuses(statuses: Iterable[str]) -> str:
    """Aggregate the statuses of several instances of a same component.

    All UP gives UP, all DOWN gives DOWN, anything else gives DEGRADED.
    An empty list gives UNKNOWN.
    """
    statuses = list(statuses)
    if not statuses:
        return Status.UNKNOWN.value
    if all(status == Status.UP.value for status in statuses):
        return Status.UP.value
    if all(status == Status.DOWN.value for status in statuses):
        return Status.DOWN.value
    return Status.DEGRADED.value
