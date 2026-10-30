import unittest

from parameterized import parameterized

from checks.status import Status, aggregate_statuses


class AggregateStatusesTestCase(unittest.TestCase):
    @parameterized.expand(
        [
            ("all_up", ["UP", "UP"], Status.UP.value),
            ("all_down", ["DOWN", "DOWN"], Status.DOWN.value),
            ("one_down", ["UP", "DOWN"], Status.DEGRADED.value),
            ("one_degraded", ["UP", "DEGRADED"], Status.DEGRADED.value),
            ("all_degraded", ["DEGRADED", "DEGRADED"], Status.DEGRADED.value),
            ("down_and_degraded", ["DOWN", "DEGRADED"], Status.DEGRADED.value),
            ("unknown_and_up", ["UP", "UNKNOWN"], Status.DEGRADED.value),
            ("single_up", ["UP"], Status.UP.value),
            ("single_down", ["DOWN"], Status.DOWN.value),
            ("empty", [], Status.UNKNOWN.value),
        ]
    )
    def test_aggregate(self, _, statuses, expected):
        self.assertEqual(aggregate_statuses(statuses), expected)
