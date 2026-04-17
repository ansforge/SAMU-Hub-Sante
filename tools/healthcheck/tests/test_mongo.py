import unittest
from unittest.mock import patch
import logging

from checks.mongo import MongoDBHealthcheck
from checks.status import Status


class TestMongoDBHealthcheck(unittest.TestCase):
    @patch("checks.mongo.mongo_client")
    def test_perform_checks_success(self, mock_mongo_client):
        # Mock successful ping
        mock_mongo_client.admin.command.return_value = {"ok": 1}

        checker = MongoDBHealthcheck()
        result = checker.perform_checks()

        # Check response
        self.assertEqual(result, {"mongodb": {"status": Status.UP.value}})

        # Check metric
        metric = checker.mongodb_status_metric
        samples = metric.collect()[0].samples
        self.assertTrue(samples, "No metric samples found")
        self.assertEqual(samples[0].value, 1.0)

    @patch("checks.mongo.mongo_client")
    def test_perform_checks_failure(self, mock_mongo_client):
        # Simulate failure
        mock_mongo_client.admin.command.side_effect = Exception("Mongo down")

        checker = MongoDBHealthcheck()

        # perform_checks should raise → handled by wrapper normally
        with self.assertRaises(Exception):
            checker.perform_checks()

    @patch("checks.mongo.mongo_client")
    def test_check_failure_fallback(self, mock_mongo_client):
        checker = MongoDBHealthcheck()

        result = checker.check_failure_fallback()

        # Check response
        self.assertEqual(result, {"mongodb": {"status": Status.DOWN.value}})

        # Check metric
        metric = checker.mongodb_status_metric
        samples = metric.collect()[0].samples
        self.assertTrue(samples, "No metric samples found")
        self.assertEqual(samples[0].value, 0.0)


class TestFunctionalMongoDBHealthcheck(unittest.TestCase):
    def setUp(self):
        # Disable logging noise like in existing tests
        logging.disable(logging.CRITICAL)

    def tearDown(self):
        logging.disable(logging.NOTSET)

    @patch("checks.mongo.mongo_client")
    def test_wrapper_success_sets_metric_to_1(self, mock_mongo_client):
        mock_mongo_client.admin.command.return_value = {"ok": 1}

        checker = MongoDBHealthcheck()
        result = checker.check_wrapper()

        # Check response
        self.assertEqual(result, {"mongodb": {"status": Status.UP.value}})

        # Check metric
        samples = checker.mongodb_status_metric.collect()[0].samples
        self.assertEqual(samples[0].value, 1.0)

    @patch("checks.mongo.mongo_client")
    def test_wrapper_failure_sets_metric_to_0(self, mock_mongo_client):
        mock_mongo_client.admin.command.side_effect = Exception("Mongo down")

        checker = MongoDBHealthcheck()
        result = checker.check_wrapper()

        # Check response
        self.assertEqual(result, {"mongodb": {"status": Status.DOWN.value}})

        # Check metric
        samples = checker.mongodb_status_metric.collect()[0].samples
        self.assertEqual(samples[0].value, 0.0)


if __name__ == "__main__":
    unittest.main()
