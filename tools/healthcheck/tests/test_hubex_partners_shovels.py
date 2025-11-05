import unittest
from unittest.mock import patch, mock_open, MagicMock
import logging

from checks.hubex_partners_shovels import (
    check_shovel_response,
    parse_monitored_shovels_config_file,
    EXPECTED_SHOVEL_STATUS,
    HubexPartnersHealthcheck,
)
from checks.status import Status


class TestHubexPartnersShovels(unittest.TestCase):
    @patch(
        "checks.hubex_partners_shovels.open",
        new_callable=mock_open,
        read_data="vhost1;queue1,queue2\nvhost2;queue3\n",
    )
    def test_parse_monitored_shovels_config_file_valid(self, *args):
        shovels_config = parse_monitored_shovels_config_file("mock_path")
        self.assertEqual(
            shovels_config, {"vhost1": ["queue1", "queue2"], "vhost2": ["queue3"]}
        )

    @patch("checks.hubex_partners_shovels.open", side_effect=FileNotFoundError)
    def test_parse_monitored_shovels_config_file_file_not_found(self, *args):
        with self.assertRaises(SystemExit) as error:
            parse_monitored_shovels_config_file("mock_path")
        self.assertIn("not found", str(error.exception))

    @patch(
        "checks.hubex_partners_shovels.open",
        new_callable=mock_open,
        read_data="invalid_line\nvhost1;queue1,queue2\n",
    )
    def test_parse_monitored_shovels_config_file_invalid_line(self, *args):
        with self.assertRaises(SystemExit) as error:
            parse_monitored_shovels_config_file("mock_path")
        self.assertIn(
            "Error reading monitored_partners_shovels.txt", str(error.exception)
        )

    def test_check_shovel_response_status_up(self):
        response = [
            {
                "vhost": "vhost1",
                "src_queue": "queue1",
                "blocked_status": EXPECTED_SHOVEL_STATUS,
            }
        ]
        result = check_shovel_response(response, "vhost1", "queue1")
        self.assertEqual(result["status"], Status.UP.value)

    def test_check_shovel_response_sttaus_down(self):
        response = [
            {"vhost": "vhost1", "src_queue": "queue1", "blocked_status": "not_running"}
        ]
        result = check_shovel_response(response, "vhost1", "queue1")
        self.assertEqual(result["status"], Status.DOWN.value)

    def test_check_shovel_response_status_down_when_missing_shovel(self):
        response = []
        with self.assertLogs(level="ERROR") as log:
            result = check_shovel_response(response, "vhost1", "queue1")
        self.assertEqual(result["status"], Status.DOWN.value)
        self.assertTrue(any("Missing shovel" in m for m in log.output))

    def test_check_shovel_response_multiple(self):
        response = [
            {"vhost": "vhost1", "src_queue": "queue1", "blocked_status": "running"},
            {"vhost": "vhost1", "src_queue": "queue1", "blocked_status": "running"},
        ]
        with self.assertLogs(level="ERROR") as log:
            result = check_shovel_response(response, "vhost1", "queue1")
        self.assertEqual(result["status"], Status.DOWN.value)
        self.assertTrue(any("Mutliple shovels" in m for m in log.output))


class TestFunctionalHubexPartnersShovels(unittest.TestCase):
    def setUp(self):
        # Prevent logging errors when the mocks throw exceptions.
        logging.disable(logging.CRITICAL)

    def tearDown(self):
        logging.disable(logging.NOTSET)

    @patch(
        "checks.hubex_partners_shovels.open",
        new_callable=mock_open,
        read_data="vhost1;queue1\n",
    )
    @patch("checks.hubex_partners_shovels.requests.get")
    def test_metric_set_to_1_on_success(self, mock_get, mock_file):
        # Mock the response from requests.get
        mock_response = MagicMock()
        mock_response.json.return_value = [
            {
                "vhost": "vhost1",
                "src_queue": "queue1",
                "blocked_status": EXPECTED_SHOVEL_STATUS,
            }
        ]
        mock_response.raise_for_status.return_value = None
        mock_get.return_value = mock_response

        # Instantiate and run the check
        checker = HubexPartnersHealthcheck()
        check_response = checker.perform_checks()

        # Check the metric value
        metric = checker.hubex_partners_status_metric
        samples = [
            s
            for s in metric.collect()[0].samples
            if s.labels.get("shovel") == "vhost1-queue1"
        ]
        self.assertTrue(samples, "Metric sample for vhost1-queue1 not found")
        self.assertEqual(samples[0].value, 1.0)

        # Check the response
        self.assertDictEqual(
            check_response,
            {
                "vhost1-queue1": {"status": Status.UP.value},
            },
        )


if __name__ == "__main__":
    unittest.main()
