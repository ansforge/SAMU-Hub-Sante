import logging
import unittest
from unittest.mock import MagicMock, patch

from parameterized import parameterized

from checks.converter import ConverterHealthcheck
from checks.status import Status


def _metric_value():
    return ConverterHealthcheck.converter_status_metric._value.get()


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

    @patch("checks.converter.http_session.get")
    def test_unreachable_converter_is_down(self, mock_get):
        mock_get.side_effect = Exception("unreachable")

        result = self.checker.check_wrapper()

        self.assertEqual(result, {"converter": {"status": Status.DOWN.value}})
        self.assertEqual(_metric_value(), 0)


if __name__ == "__main__":
    unittest.main()
