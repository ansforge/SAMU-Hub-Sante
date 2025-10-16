import unittest
from unittest.mock import patch, mock_open

from checks.hubex_partners_shovels import (
    check_shovel_response,
    parse_monitored_shovels_config_file,
    EXPECTED_SHOVEL_STATUS,
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


if __name__ == "__main__":
    unittest.main()
