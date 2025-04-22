import unittest
from unittest.mock import patch
import json
from healthcheck import app, remove_error_keys

class HealthCheckTestCase(unittest.TestCase):

    @patch("requests.get")
    def test_health_check_rabbitmq_up_and_dispatcher_up(self, mock_get):
        # Simulate RabbitMQ & dispatcher returning healthy statuses
        mock_get.return_value.status_code = 200
        mock_get.return_value.json.side_effect = [
            { "status": "ok" },
            { "status": "UP", "components": {} }
        ]
        
        # Call the route and test the response
        with patch("healthcheck.DISPATCHER_INSTANCES", ["dispatcher1"]):
            with app.test_client() as client:
                response = client.get("/health")
                self.assertEqual(response.status_code, 200)
                data = json.loads(response.data)
                self.assertEqual(data["status"], "UP")
                self.assertEqual(data["components"]["rabbitmq_server"]["status"], "UP")
                self.assertEqual(data["components"]["dispatcher1"]["status"], "UP")
    
    @patch("requests.get")
    def test_health_check_rabbitmq_down_and_dispatcher_up(self, mock_get):
        # Simulate RabbitMQ & dispatcher returning healthy statuses
        mock_get.return_value.status_code = 200
        mock_get.return_value.json.side_effect = [
            { "status": "down" },
            { "status": "UP", "components": {} }
        ]
        
        # Call the route and test the response
        with patch("healthcheck.DISPATCHER_INSTANCES", ["dispatcher1"]):
            with app.test_client() as client:
                response = client.get("/health")
                self.assertEqual(response.status_code, 200)
                data = json.loads(response.data)
                self.assertEqual(data["status"], "DOWN")
                self.assertEqual(data["components"]["rabbitmq_server"]["status"], "DOWN")
                self.assertEqual(data["components"]["dispatcher1"]["status"], "UP")
    
    @patch("requests.get")
    def test_health_check_rabbitmq_up_and_dispatcher_down(self, mock_get):
        # Simulate RabbitMQ & dispatcher returning healthy statuses
        mock_get.return_value.status_code = 200
        mock_get.return_value.json.side_effect = [
            { "status": "ok" },
            { "status": "DOWN", "components": {} }
        ]
        
        # Call the route and test the response
        with patch("healthcheck.DISPATCHER_INSTANCES", ["dispatcher1"]):
            with app.test_client() as client:
                response = client.get("/health")
                self.assertEqual(response.status_code, 200)
                data = json.loads(response.data)
                self.assertEqual(data["status"], "DOWN")
                self.assertEqual(data["components"]["rabbitmq_server"]["status"], "UP")
                self.assertEqual(data["components"]["dispatcher1"]["status"], "DOWN")

    @patch("requests.get")
    def test_health_check_rabbitmq_down_and_dispatcher_down(self, mock_get):
        # Simulate RabbitMQ & dispatcher returning healthy statuses
        mock_get.return_value.status_code = 200
        mock_get.return_value.json.side_effect = [
            { "status": "down" },
            { "status": "DOWN", "components": {} }
        ]
        
        # Call the route and test the response
        with patch("healthcheck.DISPATCHER_INSTANCES", ["dispatcher1"]):
            with app.test_client() as client:
                response = client.get("/health")
                self.assertEqual(response.status_code, 200)
                data = json.loads(response.data)
                self.assertEqual(data["status"], "DOWN")
                self.assertEqual(data["components"]["rabbitmq_server"]["status"], "DOWN")
                self.assertEqual(data["components"]["dispatcher1"]["status"], "DOWN")
    
if __name__ == '__main__':
    unittest.main()
