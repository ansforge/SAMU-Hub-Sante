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
        ([{ "status": "ok" }, { "status": "UP", "components": {} }], "UP", "UP", "UP"),
        ([{ "status": "down" }, { "status": "UP", "components": {} }], "DOWN", "DOWN", "UP"),
        ([{ "status": "ok" }, { "status": "DOWN", "components": {} }], "DOWN", "UP", "DOWN"),
        ([{ "status": "down" }, { "status": "DOWN", "components": {} }], "DOWN", "DOWN", "DOWN")
    ])
    @patch("requests.get")
    def test_health_check(self, side_effect, global_status, rabbitmq_status, dispatcher_status, mock_get):
        # Simulate RabbitMQ & dispatcher returning healthy statuses
        mock_get.return_value.status_code = 200
        mock_get.return_value.json.side_effect = side_effect
        
        # Call the route and test the response
        with patch("healthcheck.DISPATCHER_INSTANCES", ["dispatcher1"]):
            with app.test_client() as client:
                response = client.get(HEALTH_ENDPOINT)
                self.assertEqual(response.status_code, 200)
                data = json.loads(response.data)
                self.assertEqual(data["status"], global_status)
                self.assertEqual(data["components"]["rabbitmq_server"]["status"], rabbitmq_status)
                self.assertEqual(data["components"]["dispatcher1"]["status"], dispatcher_status)

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
        ([{ "status": "ok" }, { "status": "UP", "components": {} }, {"status": "UP", "components": {}}], "UP", "UP", "UP", "UP"),
        ([{ "status": "ok" }, { "status": "UP", "components": {} }, {"status": "DOWN", "components": {}}], "DOWN", "UP", "UP", "DOWN"),
    ])
    @patch("requests.get")
    def test_health_check_with_multiple_dispatchers(self, side_effect, global_status, rabbitmq_status, dispatcher1_status, dispatcher2_status, mock_get):
        # Simulate multiple dispatcher instances
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
