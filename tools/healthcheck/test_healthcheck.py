import unittest
from unittest.mock import patch
from parameterized import parameterized
import json
from healthcheck import app

class HealthCheckTestCase(unittest.TestCase):
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
                response = client.get("/health")
                self.assertEqual(response.status_code, 200)
                data = json.loads(response.data)
                self.assertEqual(data["status"], global_status)
                self.assertEqual(data["components"]["rabbitmq_server"]["status"], rabbitmq_status)
                self.assertEqual(data["components"]["dispatcher1"]["status"], dispatcher_status)
    
if __name__ == '__main__':
    unittest.main()
