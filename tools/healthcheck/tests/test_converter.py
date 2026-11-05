import logging
import unittest
from unittest.mock import MagicMock, patch

from parameterized import parameterized

from prometheus_client import REGISTRY

from checks.converter import ConverterHealthcheck
from checks.status import Status


def _metric_value():
    return ConverterHealthcheck.converter_status_metric._value.get()


def _mongodb_metric_value():
    return REGISTRY.get_sample_value("converter_mongodb_status")


class ConverterHealthcheckTestCase(unittest.TestCase):
    def setUp(self):
        logging.disable(logging.CRITICAL)
        self.checker = ConverterHealthcheck()

    def tearDown(self):
        logging.disable(logging.NOTSET)

    @parameterized.expand(
        [
            ("up", {"status": "UP"}, Status.UP.value, 1),
            ("degraded", {"status": "DEGRADED"}, Status.DEGRADED.value, 1),
            ("down", {"status": "DOWN"}, Status.DOWN.value, 0),
            ("unexpected_status", {"status": "OUT_OF_SERVICE"}, Status.DOWN.value, 0),
            ("missing_status", {}, Status.DOWN.value, 0),
        ]
    )
    @patch("checks.converter.http_session.get")
    def test_status(self, _, body, expected_status, expected_metric, mock_get):
        response = MagicMock()
        response.json.return_value = body
        mock_get.return_value = response

        result = self.checker.check_wrapper()

        self.assertEqual(result, {"converter": {"status": expected_status}})
        self.assertEqual(_metric_value(), expected_metric)

    @parameterized.expand(
        [
            ("up_mongodb_up", "UP", {"mongodb": "UP"}, 1),
            ("degraded_mongodb_down", "DEGRADED", {"mongodb": "DOWN"}, 0),
            ("mongodb_as_object", "UP", {"mongodb": {"status": "UP"}}, 1),
            ("mongodb_component_missing", "UP", {}, None),
            ("down_status", "OUT_OF_SERVICE", {"mongodb": "UP"}, None),
        ]
    )
    @patch("checks.converter.http_session.get")
    def test_mongodb_metric(self, _, status, components, expected, mock_get):
        response = MagicMock()
        response.json.return_value = {"status": status, "components": components}
        mock_get.return_value = response

        self.checker.check_wrapper()

        self.assertEqual(_mongodb_metric_value(), expected)

    @patch("checks.converter.http_session.get")
    def test_mongodb_metric_is_removed_when_converter_becomes_unreachable(
        self, mock_get
    ):
        response = MagicMock()
        response.json.return_value = {
            "status": "DEGRADED",
            "components": {"mongodb": "DOWN"},
        }
        mock_get.return_value = response
        self.checker.check_wrapper()
        self.assertEqual(_mongodb_metric_value(), 0)

        mock_get.side_effect = Exception("unreachable")
        self.checker.check_wrapper()

        self.assertIsNone(_mongodb_metric_value())
        self.assertEqual(_metric_value(), 0)

    @patch("checks.converter.http_session.get")
    def test_unreachable_converter_is_down(self, mock_get):
        mock_get.side_effect = Exception("unreachable")

        result = self.checker.check_wrapper()

        self.assertEqual(result, {"converter": {"status": Status.DOWN.value}})
        self.assertEqual(_metric_value(), 0)


if __name__ == "__main__":
    unittest.main()
