import unittest

from parameterized import parameterized

from checks.platform_status import (
    build_platform_status,
    global_status,
    perimeter_15_15_status,
    routing_15_18_status,
)

UP, DOWN, DEGRADED, UNKNOWN = "UP", "DOWN", "DEGRADED", "UNKNOWN"

PERIMETERS = {
    "d_v1": "15-15",
    "d_v2": "15-15",
    "d_v3": "15-15",
    "d_nexsis": "15-18",
}


def _platform(
    d_v1=UP,
    d_v2=UP,
    d_v3=UP,
    d_nexsis=UP,
    converter=UP,
    mongodb=UP,
    rabbitmq=UP,
    dispatchers_aggregate=None,
    fire_connections=None,
):
    statuses = {"d_v1": d_v1, "d_v2": d_v2, "d_v3": d_v3, "d_nexsis": d_nexsis}
    if dispatchers_aggregate is None:
        dispatchers_aggregate = (
            UP
            if all(s == UP for s in statuses.values())
            else (DOWN if all(s == DOWN for s in statuses.values()) else DEGRADED)
        )
    return build_platform_status(
        rabbitmq=rabbitmq,
        converter=converter,
        mongodb=mongodb,
        dispatchers_aggregate=dispatchers_aggregate,
        dispatcher_statuses=statuses,
        dispatcher_perimeters=PERIMETERS,
        fire_connections=fire_connections or {},
    )


class Perimeter1515TestCase(unittest.TestCase):
    @parameterized.expand(
        [
            ("all_up", [UP, UP, UP], UP, UP),
            ("one_down", [UP, DOWN, UP], UP, DEGRADED),
            ("all_down", [DOWN, DOWN], UP, DOWN),
            ("converter_down", [UP, UP], DOWN, DEGRADED),
            ("all_down_converter_down", [DOWN, DOWN], DOWN, DOWN),
            ("degraded_dispatcher_ignored", [UP, DEGRADED], UP, UP),
            ("no_dispatcher", [], UP, UNKNOWN),
        ]
    )
    def test_status(self, _, dispatchers, converter, expected):
        self.assertEqual(perimeter_15_15_status(dispatchers, converter), expected)


class Routing1518TestCase(unittest.TestCase):
    @parameterized.expand(
        [
            ("all_up", [UP, UP], [UP], UP, UP, UP),
            ("dispatcher_1515_down", [UP, DOWN], [UP], UP, UP, DEGRADED),
            ("dispatcher_1518_down", [UP, UP], [DOWN], UP, UP, DOWN),
            ("all_dispatchers_down", [DOWN, DOWN], [DOWN], UP, UP, DOWN),
            ("all_1515_down_1518_up", [DOWN, DOWN], [UP], UP, UP, DEGRADED),
            ("dispatcher_degraded", [UP], [DEGRADED], UP, UP, DEGRADED),
            ("converter_down", [UP], [UP], DOWN, UP, DEGRADED),
            ("mongodb_down", [UP], [UP], UP, DOWN, DEGRADED),
            ("one_of_two_1518_down", [UP], [UP, DOWN], UP, UP, DEGRADED),
            ("all_two_1518_down", [UP], [DOWN, DOWN], UP, UP, DOWN),
            ("no_1518_dispatcher", [UP], [], UP, UP, UNKNOWN),
        ]
    )
    def test_status(self, _, d15, d18, converter, mongodb, expected):
        self.assertEqual(routing_15_18_status(d15, d18, converter, mongodb), expected)


class GlobalStatusTestCase(unittest.TestCase):
    @parameterized.expand(
        [
            ("all_up", UP, [UP, UP], [UP], UP),
            ("rabbitmq_down", DOWN, [UP, UP], [UP], DOWN),
            ("all_perimeters_down", UP, [DOWN, DOWN], [UP], DOWN),
            ("one_perimeter_down", UP, [UP, DOWN], [UP], DEGRADED),
            ("one_perimeter_degraded", UP, [DEGRADED, UP], [UP], DEGRADED),
            ("technical_not_up", UP, [UP, UP], [UP, DEGRADED], DEGRADED),
            ("perimeter_unknown", UP, [UP, UNKNOWN], [UP], DEGRADED),
        ]
    )
    def test_status(self, _, rabbitmq, perimeters, technical, expected):
        self.assertEqual(global_status(rabbitmq, perimeters, technical), expected)


class BuildPlatformStatusTestCase(unittest.TestCase):
    @parameterized.expand(
        [
            ("all_up", {}, UP, UP, UP),
            ("dispatcher_v2_down", {"d_v2": DOWN}, DEGRADED, DEGRADED, DEGRADED),
            ("dispatcher_nexsis_down", {"d_nexsis": DOWN}, UP, DOWN, DEGRADED),
            (
                "all_1515_dispatchers_down",
                {"d_v1": DOWN, "d_v2": DOWN, "d_v3": DOWN},
                DOWN,
                DEGRADED,
                DEGRADED,
            ),
            (
                "mongodb_unreachable",
                {"mongodb": DOWN, "d_nexsis": DEGRADED},
                UP,
                DEGRADED,
                DEGRADED,
            ),
            ("converter_down", {"converter": DOWN}, DEGRADED, DEGRADED, DEGRADED),
            (
                "all_dispatchers_down",
                {"d_v1": DOWN, "d_v2": DOWN, "d_v3": DOWN, "d_nexsis": DOWN},
                DOWN,
                DOWN,
                DOWN,
            ),
            ("rabbitmq_down", {"rabbitmq": DOWN}, UP, UP, DOWN),
        ]
    )
    def test_scenarios(self, _, overrides, perimeter_15_15, routing, expected_global):
        result = _platform(**overrides)
        self.assertEqual(result["status"], expected_global)
        self.assertEqual(
            result["perimeters"]["15-15"]["status"],
            perimeter_15_15,
        )
        self.assertEqual(
            result["perimeters"]["15-18"]["routing"]["status"],
            routing,
        )

    def test_fire_connections_are_exposed_without_changing_the_status(self):
        result = _platform(
            fire_connections={"fire_to_health": DOWN, "health_to_fire": DEGRADED}
        )
        self.assertEqual(result["status"], UP)
        self.assertEqual(
            result["perimeters"]["15-18"]["fire_connections"],
            {
                "fire_to_health": {"status": DOWN},
                "health_to_fire": {"status": DEGRADED},
            },
        )

    def test_technical_block(self):
        result = _platform(d_v2=DOWN, mongodb=DOWN)
        self.assertEqual(
            result["technical"],
            {
                "rabbitmq": {"status": UP},
                "dispatcher": {"status": DEGRADED},
                "converter": {"status": UP},
                "persistence": {"status": DOWN},
            },
        )

    def test_dispatcher_without_perimeter_is_ignored_by_perimeters(self):
        result = build_platform_status(
            rabbitmq=UP,
            converter=UP,
            mongodb=UP,
            dispatchers_aggregate=UP,
            dispatcher_statuses={"d1": UP},
            dispatcher_perimeters={"d1": None},
            fire_connections={},
        )
        self.assertEqual(result["perimeters"]["15-15"]["status"], UNKNOWN)
        self.assertEqual(result["status"], DEGRADED)


if __name__ == "__main__":
    unittest.main()
