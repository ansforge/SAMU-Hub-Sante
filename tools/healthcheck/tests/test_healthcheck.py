import unittest
from unittest.mock import patch, mock_open
from parameterized import parameterized
import json
import requests
from checks.status import Status
from checks.annuaire import ANNUAIRE_HEALTH_URL
from checks.rabbitmq import RABBITMQ_HEALTH_URL
from checks.converter import CONVERTER_HEALTH_URL
from checks.hubex_partners_shovels import SHOVEL_STATUS_URL
import logging

with patch("checks.hubex_partners_shovels.open", mock_open(read_data="vhost1;queue1")):
    from healthcheck import HEALTH_INTERNAL_ENDPOINT, HEALTH_EXTERNAL_ENDPOINT, app

MOCK_TARGET = "http_client.http_session.get"


@patch("checks.mongo.MongoClient")
class HealthCheckTestCase(unittest.TestCase):
    def setUp(self):
        # Prevent logging errors when the mocks throw exceptions.
        logging.disable(logging.CRITICAL)

    def tearDown(self):
        logging.disable(logging.NOTSET)

    def create_mock_side_effect(
        self,
        rabbitmq_response=None,
        dispatcher_response=None,
        converter_response=None,
        annuaire_response=None,
        shovel_status_response=None,
        rabbitmq_error=False,
        dispatcher_error=False,
        converter_error=False,
        annuaire_error=False,
        shovel_status_error=False,
    ):
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
            elif ANNUAIRE_HEALTH_URL in url:
                if annuaire_error:
                    raise requests.exceptions.RequestException("Annuaire Error")
                mock_response = requests.Response()
                mock_response.status_code = 200
                response_data = annuaire_response or {"status": Status.UP.value}
                mock_response._content = json.dumps(response_data).encode()
                return mock_response
            elif SHOVEL_STATUS_URL in url:
                if shovel_status_error:
                    raise requests.exceptions.RequestException("Shovel Status Error")
                mock_response = requests.Response()
                mock_response.status_code = 200
                response_data = shovel_status_response or [
                    {
                        "vhost": "vhost1",
                        "src_queue": "queue1",
                        "blocked_status": "running",
                    },
                ]
                mock_response._content = json.dumps(response_data).encode()
                return mock_response
            else:
                if dispatcher_error:
                    raise requests.exceptions.RequestException("Dispatcher Error")
                mock_response = requests.Response()
                mock_response.status_code = 200
                response_data = dispatcher_response or {
                    "status": Status.UP.value,
                    "components": {},
                }
                mock_response._content = json.dumps(response_data).encode()
                return mock_response

        return side_effect

    @parameterized.expand(
        [
            (
                {"status": "ok"},
                {"status": "UP", "components": {}},
                {"status": "UP"},
                {"status": "UP"},
                [
                    {
                        "vhost": "vhost1",
                        "src_queue": "queue1",
                        "blocked_status": "running",
                    },
                ],
                "UP",
                "UP",
                "UP",
                "UP",
                "UP",
                "UP",
                "UP",
            ),
            (
                {"status": "down"},
                {"status": "UP", "components": {}},
                {"status": "UP"},
                {"status": "UP"},
                [
                    {
                        "vhost": "vhost1",
                        "src_queue": "queue1",
                        "blocked_status": "running",
                    },
                ],
                "DOWN",
                "DOWN",
                "UP",
                "UP",
                "UP",
                "UP",
                "UP",
            ),
            (
                {"status": "ok"},
                {"status": "DOWN", "components": {}},
                {"status": "DOWN"},
                {"status": "DOWN"},
                [
                    {
                        "vhost": "vhost1",
                        "src_queue": "queue1",
                        "blocked_status": "running",
                    },
                ],
                "DOWN",
                "UP",
                "DOWN",
                "DOWN",
                "DOWN",
                "UP",
                "UP",
            ),
            (
                {"status": "down"},
                {"status": "DOWN", "components": {}},
                {"status": "DOWN"},
                {"status": "DOWN"},
                [
                    {
                        "vhost": "vhost1",
                        "src_queue": "queue1",
                        "blocked_status": "not_running",
                    },
                ],
                "DOWN",
                "DOWN",
                "DOWN",
                "DOWN",
                "DOWN",
                "DOWN",
                "UP",
            ),
        ]
    )
    @patch(MOCK_TARGET)
    def test_health_check(
        self,
        rabbitmq_response,
        dispatcher_response,
        converter_response,
        annuaire_response,
        shovel_status_response,
        global_status,
        rabbitmq_status,
        dispatcher_status,
        converter_status,
        annuaire_status,
        shovel_status,
        mongodb_status,
        mock_get,
        mock_mongo_client,
    ):
        mock_get.side_effect = self.create_mock_side_effect(
            rabbitmq_response=rabbitmq_response,
            dispatcher_response=dispatcher_response,
            converter_response=converter_response,
            annuaire_response=annuaire_response,
            shovel_status_response=shovel_status_response,
        )

        with patch("checks.dispatcher.DISPATCHER_INSTANCES", ["dispatcher1"]):
            with app.test_client() as client:
                response = client.get(HEALTH_INTERNAL_ENDPOINT)
                self.assertEqual(response.status_code, 200)
                data = json.loads(response.data)
                self.assertEqual(data["status"], global_status)
                self.assertEqual(
                    data["components"]["rabbitmq_server"]["status"], rabbitmq_status
                )
                self.assertEqual(
                    data["components"]["dispatcher1"]["status"], dispatcher_status
                )
                self.assertEqual(
                    data["components"]["converter"]["status"], converter_status
                )
                self.assertEqual(
                    data["components"]["annuaire"]["status"], annuaire_status
                )
                self.assertEqual(
                    data["components"]["vhost1-queue1"]["status"], shovel_status
                )
                self.assertEqual(
                    data["components"]["mongodb"]["status"], mongodb_status
                )

    @patch(MOCK_TARGET)
    def test_rabbitmq_healthcheck_error(
        self,
        mock_get,
        mock_mongo_client,
    ):
        mock_get.side_effect = self.create_mock_side_effect(rabbitmq_error=True)

        with app.test_client() as client:
            response = client.get(HEALTH_INTERNAL_ENDPOINT)
            self.assertEqual(response.status_code, 200)
            data = json.loads(response.data)
            self.assertEqual(data["status"], Status.DOWN.value)
            self.assertIn("rabbitmq_server", data["components"])
            self.assertEqual(
                data["components"]["rabbitmq_server"]["status"], Status.DOWN.value
            )

    @patch(MOCK_TARGET)
    def test_converter_healthcheck_error(
        self,
        mock_get,
        mock_mongo_client,
    ):
        mock_get.side_effect = self.create_mock_side_effect(converter_error=True)

        with patch("checks.dispatcher.DISPATCHER_INSTANCES", ["dispatcher_instance"]):
            with app.test_client() as client:
                response = client.get(HEALTH_INTERNAL_ENDPOINT)
                self.assertEqual(response.status_code, 200)
                data = json.loads(response.data)
                self.assertEqual(data["status"], Status.DOWN.value)  # global status
                self.assertEqual(
                    data["components"]["converter"]["status"], Status.DOWN.value
                )

    @parameterized.expand(
        [
            (Status.UP.value, Status.UP.value, Status.UP.value),
            (Status.DOWN.value, Status.DOWN.value, Status.DOWN.value),
            (Status.UNKNOWN.value, Status.DOWN.value, Status.DOWN.value),
        ]
    )
    @patch(MOCK_TARGET)
    def test_converter_healthcheck_status(
        self,
        converter_status,
        expected_converter_status,
        expected_global_status,
        mock_get,
        mock_mongo_client,
    ):
        mock_get.side_effect = self.create_mock_side_effect(
            converter_response={"status": converter_status}
        )

        with patch("checks.dispatcher.DISPATCHER_INSTANCES", ["dispatcher_instance"]):
            with app.test_client() as client:
                response = client.get(HEALTH_INTERNAL_ENDPOINT)
                self.assertEqual(response.status_code, 200)
                data = json.loads(response.data)
                self.assertEqual(data["status"], expected_global_status)
                self.assertEqual(
                    data["components"]["converter"]["status"], expected_converter_status
                )

    @patch(MOCK_TARGET)
    def test_annuaire_healthcheck_error(
        self,
        mock_get,
        mock_mongo_client,
    ):
        mock_get.side_effect = self.create_mock_side_effect(annuaire_error=True)

        with patch("checks.dispatcher.DISPATCHER_INSTANCES", ["dispatcher_instance"]):
            with app.test_client() as client:
                response = client.get(HEALTH_INTERNAL_ENDPOINT)
                self.assertEqual(response.status_code, 200)
                data = json.loads(response.data)
                self.assertEqual(data["status"], Status.DOWN.value)  # global status
                self.assertEqual(
                    data["components"]["annuaire"]["status"], Status.DOWN.value
                )

    @parameterized.expand(
        [
            (Status.UP.value, Status.UP.value, Status.UP.value),
            (Status.DOWN.value, Status.DOWN.value, Status.DOWN.value),
            (Status.UNKNOWN.value, Status.DOWN.value, Status.DOWN.value),
        ]
    )
    @patch(MOCK_TARGET)
    def test_annuaire_healthcheck_status(
        self,
        annuaire_status,
        expected_annuaire_status,
        expected_global_status,
        mock_get,
        mock_mongo_client,
    ):
        mock_get.side_effect = self.create_mock_side_effect(
            annuaire_response={"status": annuaire_status}
        )

        with patch("checks.dispatcher.DISPATCHER_INSTANCES", ["dispatcher_instance"]):
            with app.test_client() as client:
                response = client.get(HEALTH_INTERNAL_ENDPOINT)
                self.assertEqual(response.status_code, 200)
                data = json.loads(response.data)
                self.assertEqual(data["status"], expected_global_status)
                self.assertEqual(
                    data["components"]["annuaire"]["status"], expected_annuaire_status
                )

    @parameterized.expand(
        [
            (
                {"status": "UP", "components": {}},
                {"status": "UP", "components": {}},
                {"status": "UP"},
                {"status": "UP"},
                "UP",
                "UP",
                "UP",
                "UP",
                "UP",
                "UP",
                "UP",
                "UP",
            ),
            (
                {"status": "UP", "components": {}},
                {"status": "DOWN", "components": {}},
                {"status": "DOWN"},
                {"status": "DOWN"},
                "DOWN",
                "UP",
                "UP",
                "DOWN",
                "DOWN",
                "DOWN",
                "UP",
                "UP",
            ),
        ]
    )
    @patch(MOCK_TARGET)
    def test_health_check_with_multiple_dispatchers(
        self,
        dispatcher1_response,
        dispatcher2_response,
        converter_response,
        annuaire_response,
        global_status,
        rabbitmq_status,
        dispatcher1_status,
        dispatcher2_status,
        converter_status,
        annuaire_status,
        shovel_status,
        mongodb_status,
        mock_get,
        mock_mongo_client,
    ):
        dispatcher1_name = "dispatcher1"
        dispatcher2_name = "dispatcher2"

        dispatcher_responses = {
            dispatcher1_name: dispatcher1_response,
            dispatcher2_name: dispatcher2_response,
        }

        def side_effect(url, **kwargs):
            for name, resp_data in dispatcher_responses.items():
                if name in url:
                    mock_response = requests.Response()
                    mock_response.status_code = 200
                    mock_response._content = json.dumps(resp_data).encode()
                    return mock_response
            return self.create_mock_side_effect(
                converter_response=converter_response,
                annuaire_response=annuaire_response,
            )(url, **kwargs)

        mock_get.side_effect = side_effect

        with patch(
            "checks.dispatcher.DISPATCHER_INSTANCES",
            [dispatcher1_name, dispatcher2_name],
        ):
            with app.test_client() as client:
                response = client.get(HEALTH_INTERNAL_ENDPOINT)
                self.assertEqual(response.status_code, 200)
                data = json.loads(response.data)
                self.assertEqual(data["status"], global_status)
                self.assertEqual(
                    data["components"]["rabbitmq_server"]["status"], rabbitmq_status
                )
                self.assertEqual(
                    data["components"][dispatcher1_name]["status"], dispatcher1_status
                )
                self.assertEqual(
                    data["components"][dispatcher2_name]["status"], dispatcher2_status
                )
                self.assertEqual(
                    data["components"]["converter"]["status"], converter_status
                )
                self.assertEqual(
                    data["components"]["annuaire"]["status"], annuaire_status
                )
                self.assertEqual(
                    data["components"]["vhost1-queue1"]["status"], shovel_status
                )
                self.assertEqual(
                    data["components"]["mongodb"]["status"], mongodb_status
                )

    @patch(MOCK_TARGET)
    def test_external_health_check(
        self,
        mock_get,
        mock_mongo_client,
    ):
        mock_get.side_effect = self.create_mock_side_effect()

        with patch("checks.dispatcher.DISPATCHER_INSTANCES", ["dispatcher_instance"]):
            with app.test_client() as client:
                response = client.get(HEALTH_EXTERNAL_ENDPOINT)
                self.assertEqual(response.status_code, 200)
                data = json.loads(response.data)
                self.assertEqual(data["status"], Status.UP.value)
                self.assertEqual(len(data["components"]), 5)
                self.assertEqual(
                    data["components"]["rabbitmq_server"]["status"], Status.UP.value
                )
                self.assertEqual(
                    data["components"]["dispatcher_instance"]["status"],
                    Status.UP.value,
                )
                self.assertEqual(
                    data["components"]["converter"]["status"], Status.UP.value
                )
                self.assertEqual(
                    data["components"]["vhost1-queue1"]["status"], Status.UP.value
                )
                self.assertEqual(
                    data["components"]["mongodb"]["status"], Status.UP.value
                )
                self.assertNotIn("annuaire", data["components"].keys())

    @patch(MOCK_TARGET)
    def test_update_metrics_before_scrapping_requests_are_made(
        self,
        mock_get,
        mock_mongo_client,
    ):
        mock_get.side_effect = self.create_mock_side_effect()

        with patch(
            "checks.dispatcher.DISPATCHER_INSTANCES", ["dispatcher1", "dispatcher2"]
        ):
            with app.test_client() as client:
                client.get("/metrics")

            called_urls = [call_args[0][0] for call_args in mock_get.call_args_list]

            # 6 calls for: converter, annuaire, rabbitmq, shovel status and the 2 dispatchers
            expected_numbers_of_requests = 6
            self.assertEqual(len(called_urls), expected_numbers_of_requests)
            self.assertIn(RABBITMQ_HEALTH_URL, called_urls)
            self.assertIn(CONVERTER_HEALTH_URL, called_urls)
            self.assertIn(ANNUAIRE_HEALTH_URL, called_urls)
            self.assertIn(SHOVEL_STATUS_URL, called_urls)
            self.assertIn(
                "http://dispatcher1.app.svc.cluster.local:8080/actuator/health",
                called_urls,
            )
            self.assertIn(
                "http://dispatcher2.app.svc.cluster.local:8080/actuator/health",
                called_urls,
            )

    @patch("requests.get")
    def test_mongodb_healthcheck_up(
        self,
        mock_get,
        mock_mongo_client,
    ):
        mock_get.side_effect = self.create_mock_side_effect()

        with patch("checks.dispatcher.DISPATCHER_INSTANCES", ["dispatcher_instance"]):
            with app.test_client() as client:
                response = client.get(HEALTH_INTERNAL_ENDPOINT)
                self.assertEqual(response.status_code, 200)
                data = json.loads(response.data)
                self.assertEqual(
                    data["components"]["mongodb"]["status"], Status.UP.value
                )
                self.assertEqual(
                    mock_mongo_client.return_value.admin.command.called, True
                )
                mock_mongo_client.return_value.admin.command.assert_called_with("ping")

    @patch("requests.get")
    def test_mongodb_healthcheck_error(
        self,
        mock_get,
        mock_mongo_client,
    ):
        mock_get.side_effect = self.create_mock_side_effect()
        mock_mongo_client.return_value.admin.command.side_effect = Exception(
            "MongoDB Error"
        )

        with patch("checks.dispatcher.DISPATCHER_INSTANCES", ["dispatcher_instance"]):
            with app.test_client() as client:
                response = client.get(HEALTH_INTERNAL_ENDPOINT)
                self.assertEqual(response.status_code, 200)
                data = json.loads(response.data)
                self.assertEqual(data["status"], Status.DOWN.value)
                self.assertEqual(
                    data["components"]["mongodb"]["status"], Status.DOWN.value
                )


if __name__ == "__main__":
    unittest.main()
