import unittest
from unittest.mock import patch, mock_open
import requests
import json

import checks.hubex_partners_shovels as shovels_module
from checks.status import Status
from healthcheck import HEALTH_EXTERNAL_ENDPOINT, app


class TestHubexPartnersShovels(unittest.TestCase):
    def create_mock_side_effect(self, error=False, response_data=None):
        def side_effect(**kwargs):
            if error:
                raise requests.exceptions.RequestException("Error")
            mock_response = requests.Response()
            mock_response.status_code = 200
            mock_response._content = json.dumps(error or response_data).encode()
            return mock_response

        return side_effect

    @patch(
        "builtins.open", mock_open(read_data="vhost1;queue1,queue2\nvhost2;queue3\n")
    )
    def test_parse_monitored_shovels_config_file_valid(self, **mocks):
        shovels_config = shovels_module.parse_monitored_shovels_config_file("mock_path")
        self.assertEqual(
            shovels_config, {"vhost1": ["queue1", "queue2"], "vhost2": ["queue3"]}
        )

    @patch("builtins.open", side_effect=FileNotFoundError)
    def test_parse_monitored_shovels_config_file_file_not_found(self, *args):
        with self.assertRaises(SystemExit) as error:
            shovels_module.parse_monitored_shovels_config_file("mock_path")
        self.assertIn("not found", str(error.exception))

    @patch("builtins.open", mock_open(read_data="invalid_line\nvhost1;queue1,queue2\n"))
    def test_parse_monitored_shovels_config_file_invalid_line(self, *args):
        with self.assertRaises(SystemExit) as error:
            shovels_module.parse_monitored_shovels_config_file("mock_path")
        self.assertIn(
            "Error reading monitored_partners_shovels.txt", str(error.exception)
        )

    def test_check_shovel_response_up(self):
        response = [
            {
                "vhost": "vhost1",
                "src_queue": "queue1",
                "blocked_status": shovels_module.EXPECTED_SHOVEL_STATUS,
            }
        ]
        result = shovels_module.check_shovel_response(response, "vhost1", "queue1")
        self.assertEqual(result["status"], shovels_module.Status.UP.value)

    def test_check_shovel_response_down(self):
        response = [
            {"vhost": "vhost1", "src_queue": "queue1", "blocked_status": "not_running"}
        ]
        result = shovels_module.check_shovel_response(response, "vhost1", "queue1")
        self.assertEqual(result["status"], shovels_module.Status.DOWN.value)

    def test_check_shovel_response_missing(self):
        response = []
        with self.assertLogs(level="ERROR") as log:
            result = shovels_module.check_shovel_response(response, "vhost1", "queue1")
        self.assertEqual(result["status"], shovels_module.Status.DOWN.value)
        self.assertTrue(any("Missing shovel" in m for m in log.output))

    def test_check_shovel_response_multiple(self):
        response = [
            {"vhost": "vhost1", "src_queue": "queue1", "blocked_status": "running"},
            {"vhost": "vhost1", "src_queue": "queue1", "blocked_status": "running"},
        ]
        with self.assertLogs(level="ERROR") as log:
            result = shovels_module.check_shovel_response(response, "vhost1", "queue1")
        self.assertEqual(result["status"], shovels_module.Status.DOWN.value)
        self.assertTrue(any("Mutliple shovels" in m for m in log.output))

    @patch("requests.get")
    @patch("checks.hubex_partners_shovels.hubex_partners_status_metric")
    @patch.object(shovels_module, "SHOVELS_MAP", {"vhost1": ["queue1"]})
    def test_hubex_partners_shovels_healthcheck_success(self, mock_get, mock_metric):
        mock_get.side_effect = self.create_mock_side_effect(
            response_data=[
                {
                    "vhost": "vhost1",
                    "src_queue": "queue1",
                    "blocked_status": shovels_module.EXPECTED_SHOVEL_STATUS,
                }
            ]
        )

        with app.test_client() as client:
            response = client.get(HEALTH_EXTERNAL_ENDPOINT)
            self.assertEqual(response.status_code, 200)
            data = json.loads(response.data)
            self.assertEqual(data["status"], Status.UP.value)
            self.assertIn("vhost1-queue1", data["components"])
            self.assertEqual(
                data["components"]["vhost1-queue1"]["status"], Status.UP.value
            )
            mock_metric.labels.assert_called_with(dispatcher="vhost1-queue1")

    @patch("requests.get")
    @patch("checks.hubex_partners_shovels.hubex_partners_status_metric")
    @patch.object(shovels_module, "SHOVELS_MAP", {"vhost1": ["queue1"]})
    def test_hubex_partners_shovels_healthcheck_failure(self, mock_get, mock_metric):
        mock_get.side_effect = self.create_mock_side_effect(
            response_data=[
                {
                    "vhost": "vhost1",
                    "src_queue": "queue1",
                    "blocked_status": shovels_module.EXPECTED_SHOVEL_STATUS,
                }
            ]
        )

        with app.test_client() as client:
            response = client.get(HEALTH_EXTERNAL_ENDPOINT)
            self.assertEqual(response.status_code, 200)
            data = json.loads(response.data)
            self.assertEqual(data["status"], Status.DOWN.value)
            self.assertIn("vhost1-queue1", data["components"])
            self.assertEqual(
                data["components"]["vhost1-queue1"]["status"], Status.DOWN.value
            )
            mock_metric.labels.assert_called_with(dispatcher="vhost1-queue1")
            mock_metric.labels.assert_called_with(dispatcher="vhost1-queue1")


if __name__ == "__main__":
    unittest.main()
