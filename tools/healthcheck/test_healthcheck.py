import unittest
from unittest.mock import patch
from parameterized import parameterized
import json
import requests
from healthcheck import CONVERTER_HEALTH_URL, HEALTH_ENDPOINT, RABBITMQ_HEALTH_URL, Status, app, remove_error_keys
import logging

class HealthCheckTestCase(unittest.TestCase):
    def setUp(self):
        # Prevent logging errors when the mocks throw exceptions.
        logging.disable(logging.CRITICAL)

    def tearDown(self):
        logging.disable(logging.NOTSET)

    @parameterized.expand([
        ([{ "status": "ok" }, { "status": "UP", "components": {} }, { "status": "UP" }], "UP", "UP", "UP", "UP"),
        ([{ "status": "down" }, { "status": "UP", "components": {} }, { "status": "UP" }], "DOWN", "DOWN", "UP", "UP"),
        ([{ "status": "ok" }, { "status": "DOWN", "components": {} }, { "status": "DOWN" }], "DOWN", "UP", "DOWN", "DOWN"),
        ([{ "status": "down" }, { "status": "DOWN", "components": {} }, { "status": "DOWN" }], "DOWN", "DOWN", "DOWN", "DOWN")
    ])
    @patch("requests.get")
    def test_health_check(self, side_effect, global_status, rabbitmq_status, dispatcher_status, converter_status, mock_get):
        mock_get.return_value.status_code = 200
        mock_get.return_value.json.side_effect = side_effect

        with patch("healthcheck.DISPATCHER_INSTANCES", ["dispatcher1"]):
            with app.test_client() as client:
                response = client.get(HEALTH_ENDPOINT)
                self.assertEqual(response.status_code, 200)
                data = json.loads(response.data)
                self.assertEqual(data["status"], global_status)
                self.assertEqual(data["components"]["rabbitmq_server"]["status"], rabbitmq_status)
                self.assertEqual(data["components"]["dispatcher1"]["status"], dispatcher_status)
                self.assertEqual(data["components"]["converter"]["status"], converter_status)

    @patch("requests.get")
    def test_rabbitmq_healthcheck_error(self, mock_get):
        # Simulate an error while trying to access RabbitMQ
        mock_get.side_effect = requests.exceptions.RequestException("Error")
        
        # Call the route and test the response
        with app.test_client() as client:
            response = client.get(HEALTH_ENDPOINT)
            self.assertEqual(response.status_code, 200)
            data = json.loads(response.data)
            self.assertEqual(data["status"], Status.DOWN.value)
            self.assertIn("rabbitmq_server", data["components"])
            self.assertEqual(data["components"]["rabbitmq_server"]["status"], Status.DOWN.value)
    @parameterized.expand([
        ([{ "status": "ok" }, { "status": "UP", "components": {} }, {"status": "UP", "components": {}}, { "status": "UP" }], "UP", "UP", "UP", "UP", "UP"),
        ([{ "status": "ok" }, { "status": "UP", "components": {} }, {"status": "DOWN", "components": {}}, { "status": "DOWN" }], "DOWN", "UP", "UP", "DOWN", "DOWN"),
    ])
    @patch("requests.get")
    def test_health_check_with_multiple_dispatchers(self, side_effect, global_status, rabbitmq_status, dispatcher1_status, dispatcher2_status, converter_status, mock_get):
        mock_get.return_value.status_code = 200
        mock_get.return_value.json.side_effect = side_effect

        with patch("healthcheck.DISPATCHER_INSTANCES", ["dispatcher1", "dispatcher2"]):
            with app.test_client() as client:
                response = client.get(HEALTH_ENDPOINT)
                self.assertEqual(response.status_code, 200)
                data = json.loads(response.data)
                self.assertEqual(data["status"], global_status)
                self.assertEqual(data["components"]["rabbitmq_server"]["status"], rabbitmq_status)
                self.assertEqual(data["components"]["dispatcher1"]["status"], dispatcher1_status)
                self.assertEqual(data["components"]["dispatcher2"]["status"], dispatcher2_status)
                self.assertEqual(data["components"]["converter"]["status"], converter_status)

    def test_remove_error_keys(self):
        # Test if error keys are properly removed
        data = {
            "status": Status.UP.value,
            "components": {
                "rabbitmq_server": {
                    "status": Status.UP.value
                },
                "dispatcher1": {
                    "status":Status.DOWN.value,
                    "error": "Dispatcher failed"
                }
            }
        }
        result = remove_error_keys(data)
        self.assertNotIn("error", result["components"]["dispatcher1"])
        self.assertEqual(result["status"], Status.UP.value)


if __name__ == '__main__':
    unittest.main()
