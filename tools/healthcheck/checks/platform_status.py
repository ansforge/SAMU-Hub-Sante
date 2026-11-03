from typing import Dict, Iterable, List, Optional

from checks.dispatcher import PERIMETER_15_15, PERIMETER_15_18
from checks.status import Status

UP = Status.UP.value
DOWN = Status.DOWN.value
DEGRADED = Status.DEGRADED.value
UNKNOWN = Status.UNKNOWN.value


def _all_down(statuses: Iterable[str]) -> bool:
    statuses = list(statuses)
    return bool(statuses) and all(status == DOWN for status in statuses)


def perimeter_15_15_status(dispatchers: List[str], converter: str) -> str:
    """Status of the 15-15 and 15-CAP flows.

    A message is converted by the dispatchers, which call the converter: the
    perimeter is DOWN when all its dispatchers are DOWN, and DEGRADED when some
    flows are blocked (a dispatcher or the converter is DOWN). A DEGRADED
    dispatcher (MongoDB unreachable) does not affect this perimeter.
    """
    if not dispatchers:
        return UNKNOWN
    if _all_down(dispatchers):
        return DOWN
    if DOWN in dispatchers or converter == DOWN:
        return DEGRADED
    return UP


def routing_15_18_status(
    dispatchers_15_15: List[str],
    dispatchers_15_18: List[str],
    converter: str,
    mongodb: str,
) -> str:
    """Status of the routing components of the 15-18 flows.

    Every 15-18 flow goes through the 15-18 dispatcher, so the routing is DOWN
    when it is DOWN (all of them if there are several), or when all the
    dispatchers are DOWN. It is DEGRADED when a dispatcher is not UP, when the
    converter is DOWN or when MongoDB is unreachable.
    """
    if not dispatchers_15_18:
        return UNKNOWN
    all_dispatchers = dispatchers_15_15 + dispatchers_15_18
    if _all_down(dispatchers_15_18) or _all_down(all_dispatchers):
        return DOWN
    if any(status != UP for status in all_dispatchers):
        return DEGRADED
    if converter == DOWN or mongodb == DOWN:
        return DEGRADED
    return UP


def global_status(
    rabbitmq: str, perimeters: Iterable[str], technical: Iterable[str]
) -> str:
    """DOWN if the broker or all the perimeters are DOWN, DEGRADED if a
    perimeter or a technical component is not UP, UP otherwise."""
    perimeters = list(perimeters)
    if rabbitmq == DOWN or _all_down(perimeters):
        return DOWN
    if any(status != UP for status in perimeters + list(technical)):
        return DEGRADED
    return UP


def build_platform_status(
    *,
    rabbitmq: str,
    converter: str,
    mongodb: str,
    dispatchers_aggregate: str,
    dispatcher_statuses: Dict[str, str],
    dispatcher_perimeters: Dict[str, Optional[str]],
    fire_connections: Dict[str, str],
) -> dict:
    """Build the global status and the perimeters and technical blocks.

    The connections with the Hubex fire are only exposed: they depend on the
    partner, so they change neither the routing nor the global status. They are
    omitted when there is nothing to monitor.
    """

    def statuses_of(perimeter: str) -> List[str]:
        return [
            status
            for name, status in dispatcher_statuses.items()
            if dispatcher_perimeters.get(name) == perimeter
        ]

    dispatchers_15_15 = statuses_of(PERIMETER_15_15)
    dispatchers_15_18 = statuses_of(PERIMETER_15_18)

    status_15_15 = perimeter_15_15_status(dispatchers_15_15, converter)
    routing_15_18 = routing_15_18_status(
        dispatchers_15_15, dispatchers_15_18, converter, mongodb
    )
    technical = {
        "rabbitmq": rabbitmq,
        "dispatcher": dispatchers_aggregate,
        "converter": converter,
        "persistence": mongodb,
    }

    perimeter_15_18 = {"routing": {"status": routing_15_18}}
    if fire_connections:
        perimeter_15_18["fire_connections"] = {
            name: {"status": status} for name, status in fire_connections.items()
        }

    return {
        "status": global_status(
            rabbitmq, [status_15_15, routing_15_18], technical.values()
        ),
        "perimeters": {
            PERIMETER_15_15: {"status": status_15_15},
            PERIMETER_15_18: perimeter_15_18,
        },
        "technical": {name: {"status": status} for name, status in technical.items()},
    }
