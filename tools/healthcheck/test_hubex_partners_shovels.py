import unittest
from unittest.mock import patch, mock_open
import requests
import json

from checks.hubex_partners_shovels import (
    check_shovel_response,
    parse_monitored_shovels_config_file,
    EXPECTED_SHOVEL_STATUS,
)
from checks.status import Status


class TestHubexPartnersShovels(unittest.TestCase):
    def create_mock_side_effect(self, error=False, response_data=None):
        def side_effect(**kwargs):
            if error:
                raise requests.exceptions.RequestException("Error")
            mock_response = requests.Response()
            mock_response.status_code = 200
            mock_response._content = json.dumps(response_data or []).encode()
            return mock_response

        return side_effect

    @patch(
        "builtins.open", mock_open(read_data="vhost1;queue1,queue2\nvhost2;queue3\n")
    )
    def test_parse_monitored_shovels_config_file_valid(self, **mocks):
        shovels_config = parse_monitored_shovels_config_file("mock_path")
        self.assertEqual(
            shovels_config, {"vhost1": ["queue1", "queue2"], "vhost2": ["queue3"]}
        )

    @patch("builtins.open", side_effect=FileNotFoundError)
    def test_parse_monitored_shovels_config_file_file_not_found(self, *args):
        with self.assertRaises(SystemExit) as error:
            parse_monitored_shovels_config_file("mock_path")
        self.assertIn("not found", str(error.exception))

    @patch("builtins.open", mock_open(read_data="invalid_line\nvhost1;queue1,queue2\n"))
    def test_parse_monitored_shovels_config_file_invalid_line(self, *args):
        with self.assertRaises(SystemExit) as error:
            parse_monitored_shovels_config_file("mock_path")
        self.assertIn(
            "Error reading monitored_partners_shovels.txt", str(error.exception)
        )

    def test_check_shovel_response_up(self):
        response = [
            {
                "vhost": "vhost1",
                "src_queue": "queue1",
                "blocked_status": EXPECTED_SHOVEL_STATUS,
            }
        ]
        result = check_shovel_response(response, "vhost1", "queue1")
        self.assertEqual(result["status"], Status.UP.value)

    def test_check_shovel_response_down(self):
        response = [
            {"vhost": "vhost1", "src_queue": "queue1", "blocked_status": "not_running"}
        ]
        result = check_shovel_response(response, "vhost1", "queue1")
        self.assertEqual(result["status"], Status.DOWN.value)

    def test_check_shovel_response_missing(self):
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


if __name__ == "__main__":
    unittest.main()
