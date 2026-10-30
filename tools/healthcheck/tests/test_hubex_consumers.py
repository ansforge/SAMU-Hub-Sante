import unittest
from unittest.mock import patch, mock_open, MagicMock
import logging

from checks.hubex_consumers import (
    check_queue_consumers,
    parse_monitored_queues_config_file,
    HubexConsumersHealthcheck,
)
from checks.status import Status


class TestParseMonitoredQueuesConfigFile(unittest.TestCase):
    @patch(
        "checks.hubex_consumers.open",
        new_callable=mock_open,
        read_data="vhost1;queue1,queue2\n\nvhost2;queue3\n",
    )
    def test_parse_valid(self, *args):
        queues_config = parse_monitored_queues_config_file("mock_path")
        self.assertEqual(
            queues_config, {"vhost1": ["queue1", "queue2"], "vhost2": ["queue3"]}
        )

    @patch("checks.hubex_consumers.open", side_effect=FileNotFoundError)
    def test_parse_file_not_found(self, *args):
        with self.assertRaises(SystemExit) as error:
            parse_monitored_queues_config_file("mock_path")
        self.assertIn("not found", str(error.exception))

    @patch(
        "checks.hubex_consumers.open",
        new_callable=mock_open,
        read_data="invalid_line\nvhost1;queue1\n",
    )
    def test_parse_invalid_line(self, *args):
        with self.assertRaises(SystemExit) as error:
            parse_monitored_queues_config_file("mock_path")
        self.assertIn("Error reading monitored_hubex_queues.txt", str(error.exception))


class TestCheckQueueConsumers(unittest.TestCase):
    def setUp(self):
        logging.disable(logging.CRITICAL)

    def tearDown(self):
        logging.disable(logging.NOTSET)

    def test_up_when_at_least_one_consumer(self):
        data = [{"vhost": "vhost1", "name": "queue1", "consumers": 2}]
        result = check_queue_consumers(data, "vhost1", "queue1")
        self.assertEqual(result["status"], Status.UP.value)

    def test_down_when_no_consumer(self):
        data = [{"vhost": "vhost1", "name": "queue1", "consumers": 0}]
        result = check_queue_consumers(data, "vhost1", "queue1")
        self.assertEqual(result["status"], Status.DOWN.value)

    def test_down_when_queue_missing(self):
        data = [{"vhost": "vhost1", "name": "other", "consumers": 1}]
        result = check_queue_consumers(data, "vhost1", "queue1")
        self.assertEqual(result["status"], Status.DOWN.value)

    def test_down_when_same_queue_name_on_other_vhost_only(self):
        data = [{"vhost": "vhost2", "name": "queue1", "consumers": 1}]
        result = check_queue_consumers(data, "vhost1", "queue1")
        self.assertEqual(result["status"], Status.DOWN.value)

    def test_down_when_consumers_field_missing(self):
        data = [{"vhost": "vhost1", "name": "queue1"}]
        result = check_queue_consumers(data, "vhost1", "queue1")
        self.assertEqual(result["status"], Status.DOWN.value)

    def test_down_when_consumers_field_invalid(self):
        data = [{"vhost": "vhost1", "name": "queue1", "consumers": "1"}]
        result = check_queue_consumers(data, "vhost1", "queue1")
        self.assertEqual(result["status"], Status.DOWN.value)

    def test_down_when_multiple_queues_found(self):
        data = [
            {"vhost": "vhost1", "name": "queue1", "consumers": 1},
            {"vhost": "vhost1", "name": "queue1", "consumers": 1},
        ]
        result = check_queue_consumers(data, "vhost1", "queue1")
        self.assertEqual(result["status"], Status.DOWN.value)

    def test_ignores_malformed_elements(self):
        data = ["unexpected", {"vhost": "vhost1", "name": "queue1", "consumers": 1}]
        result = check_queue_consumers(data, "vhost1", "queue1")
        self.assertEqual(result["status"], Status.UP.value)


class TestHubexConsumersHealthcheck(unittest.TestCase):
    def setUp(self):
        logging.disable(logging.CRITICAL)
        with patch(
            "checks.hubex_consumers.open",
            mock_open(read_data="vhost1;queue-message,queue-ack"),
        ):
            self.checker = HubexConsumersHealthcheck()

    def tearDown(self):
        logging.disable(logging.NOTSET)

    def _metric_value(self, queue_label):
        return HubexConsumersHealthcheck.hubex_consumers_status_metric.labels(
            queue=queue_label
        )._value.get()

    @patch("checks.hubex_consumers.http_session.get")
    def test_perform_checks_all_queues_consumed(self, mock_get):
        mock_get.return_value = MagicMock(
            json=lambda: [
                {"vhost": "vhost1", "name": "queue-message", "consumers": 1},
                {"vhost": "vhost1", "name": "queue-ack", "consumers": 1},
            ]
        )
        result = self.checker.check_wrapper()
        self.assertEqual(
            result,
            {
                "vhost1-queue-message-consumers": {"status": Status.UP.value},
                "vhost1-queue-ack-consumers": {"status": Status.UP.value},
                "vhost1-consumers": {"status": Status.UP.value},
            },
        )
        self.assertEqual(self._metric_value("vhost1-queue-message"), 1)
        self.assertEqual(self._metric_value("vhost1-queue-ack"), 1)

    @patch("checks.hubex_consumers.http_session.get")
    def test_perform_checks_one_queue_without_consumer(self, mock_get):
        mock_get.return_value = MagicMock(
            json=lambda: [
                {"vhost": "vhost1", "name": "queue-message", "consumers": 1},
                {"vhost": "vhost1", "name": "queue-ack", "consumers": 0},
            ]
        )
        result = self.checker.check_wrapper()
        self.assertEqual(
            result["vhost1-queue-message-consumers"]["status"], Status.UP.value
        )
        self.assertEqual(
            result["vhost1-queue-ack-consumers"]["status"], Status.DOWN.value
        )
        self.assertEqual(result["vhost1-consumers"]["status"], Status.DEGRADED.value)
        self.assertEqual(self._metric_value("vhost1-queue-message"), 1)
        self.assertEqual(self._metric_value("vhost1-queue-ack"), 0)

    @patch("checks.hubex_consumers.http_session.get")
    def test_perform_checks_without_monitored_queues_skips_api_call(self, mock_get):
        with patch("checks.hubex_consumers.open", mock_open(read_data="")):
            checker = HubexConsumersHealthcheck()
        result = checker.check_wrapper()
        self.assertEqual(result, {})
        mock_get.assert_not_called()

    @patch("checks.hubex_consumers.http_session.get")
    def test_check_wrapper_falls_back_to_down_on_api_error(self, mock_get):
        mock_get.side_effect = Exception("RabbitMQ unreachable")
        result = self.checker.check_wrapper()
        self.assertEqual(
            result,
            {
                "vhost1-queue-message-consumers": {"status": Status.DOWN.value},
                "vhost1-queue-ack-consumers": {"status": Status.DOWN.value},
                "vhost1-consumers": {"status": Status.DOWN.value},
            },
        )
        self.assertEqual(self._metric_value("vhost1-queue-message"), 0)
        self.assertEqual(self._metric_value("vhost1-queue-ack"), 0)
