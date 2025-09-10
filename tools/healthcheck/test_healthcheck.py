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

    def create_mock_side_effect(self, rabbitmq_response=None, dispatcher_response=None, converter_response=None,
                               rabbitmq_error=False, dispatcher_error=False, converter_error=False):
        def side_effect(url, **kwargs):
            if CONVERTER_HEALTH_URL in url:
                if converter_error:
                    raise requests.exceptions.RequestException("Converter Error")
                mock_response = requests.Response()
                mock_response.status_code = 200
                response_data = converter_response or {"status": Status.UP.value}
                mock_response._content = json.dumps(response_data).encode()
                return mock_response
            elif RABBITMQ_HEALTH_URL in url:
                if rabbitmq_error:
                    raise requests.exceptions.RequestException("RabbitMQ Error")
                mock_response = requests.Response()
                mock_response.status_code = 200
                response_data = rabbitmq_response or {"status": Status.OK.value}
                mock_response._content = json.dumps(response_data).encode()
                return mock_response
            else:
                if dispatcher_error:
                    raise requests.exceptions.RequestException("Dispatcher Error")
                mock_response = requests.Response()
                mock_response.status_code = 200
                response_data = dispatcher_response or {"status": Status.UP.value, "components": {}}
                mock_response._content = json.dumps(response_data).encode()
                return mock_response

        return side_effect

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
        mock_get.side_effect = self.create_mock_side_effect(rabbitmq_error=True)

        with app.test_client() as client:
            response = client.get(HEALTH_ENDPOINT)
            self.assertEqual(response.status_code, 200)
            data = json.loads(response.data)
            self.assertEqual(data["status"], Status.DOWN.value)
            self.assertIn("rabbitmq_server", data["components"])
            self.assertEqual(data["components"]["rabbitmq_server"]["status"], Status.DOWN.value)

    @patch("requests.get")
    def test_converter_healthcheck_error(self, mock_get):
        mock_get.side_effect = self.create_mock_side_effect(converter_error=True)

        with patch("healthcheck.DISPATCHER_INSTANCES", ["dispatcher_instance"]):
            with app.test_client() as client:
                response = client.get(HEALTH_ENDPOINT)
                self.assertEqual(response.status_code, 200)
                data = json.loads(response.data)
                self.assertEqual(data["status"], Status.DOWN.value) # global status
                self.assertEqual(data["components"]["converter"]["status"], Status.DOWN.value)

    @parameterized.expand([
        (Status.UP.value, Status.UP.value, Status.UP.value),
        (Status.DOWN.value, Status.DOWN.value, Status.DOWN.value),
        (Status.UNKNOWN.value, Status.DOWN.value, Status.DOWN.value),
    ])
    @patch("requests.get")
    def test_converter_healthcheck_status(self, converter_status, expected_converter_status, expected_global_status, mock_get):
        mock_get.side_effect = self.create_mock_side_effect(converter_response={"status": converter_status})

        with patch("healthcheck.DISPATCHER_INSTANCES", ["dispatcher_instance"]):
            with app.test_client() as client:
                response = client.get(HEALTH_ENDPOINT)
                self.assertEqual(response.status_code, 200)
                data = json.loads(response.data)
                self.assertEqual(data["status"], expected_global_status)
                self.assertEqual(data["components"]["converter"]["status"], expected_converter_status)

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
