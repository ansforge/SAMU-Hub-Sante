import unittest
from unittest.mock import patch, MagicMock
import logging

from checks.mongo import MongoDBHealthcheck
from checks.status import Status


class TestMongoDBHealthcheck(unittest.TestCase):
    @patch("checks.mongo.MongoClient")
    def test_perform_checks_success(self, mock_mongo_client):
        mock_instance = MagicMock()
        mock_instance.admin.command.return_value = {"ok": 1}
        mock_mongo_client.return_value = mock_instance

        checker = MongoDBHealthcheck()
        result = checker.perform_checks()

        self.assertEqual(result, {"mongodb": {"status": Status.UP.value}})

        metric = checker.mongodb_status_metric
        samples = metric.collect()[0].samples
        self.assertTrue(samples, "No metric samples found")
        self.assertEqual(samples[0].value, 1.0)

    @patch("checks.mongo.MongoClient")
    def test_perform_checks_failure(self, mock_mongo_client):
        mock_instance = MagicMock()
        mock_instance.admin.command.side_effect = Exception("Mongo down")
        mock_mongo_client.return_value = mock_instance

        checker = MongoDBHealthcheck()

        with self.assertRaises(Exception):
            checker.perform_checks()

    @patch("checks.mongo.MongoClient")
    def test_check_failure_fallback(self, mock_mongo_client):
        checker = MongoDBHealthcheck()

        result = checker.check_failure_fallback()

        self.assertEqual(result, {"mongodb": {"status": Status.DOWN.value}})

        metric = checker.mongodb_status_metric
        samples = metric.collect()[0].samples
        self.assertTrue(samples, "No metric samples found")
        self.assertEqual(samples[0].value, 0.0)


class TestFunctionalMongoDBHealthcheck(unittest.TestCase):
    def setUp(self):
        logging.disable(logging.CRITICAL)

    def tearDown(self):
        logging.disable(logging.NOTSET)

    @patch("checks.mongo.MongoClient")
    def test_wrapper_success_sets_metric_to_1(self, mock_mongo_client):
        mock_instance = MagicMock()
        mock_instance.admin.command.return_value = {"ok": 1}
        mock_mongo_client.return_value = mock_instance

        checker = MongoDBHealthcheck()
        result = checker.check_wrapper()

        self.assertEqual(result, {"mongodb": {"status": Status.UP.value}})

        samples = checker.mongodb_status_metric.collect()[0].samples
        self.assertEqual(samples[0].value, 1.0)

    @patch("checks.mongo.MongoClient")
    def test_wrapper_failure_sets_metric_to_0(self, mock_mongo_client):
        mock_instance = MagicMock()
        mock_instance.admin.command.side_effect = Exception("Mongo down")
        mock_mongo_client.return_value = mock_instance

        checker = MongoDBHealthcheck()
        result = checker.check_wrapper()

        self.assertEqual(result, {"mongodb": {"status": Status.DOWN.value}})

        samples = checker.mongodb_status_metric.collect()[0].samples
        self.assertEqual(samples[0].value, 0.0)


if __name__ == "__main__":
    unittest.main()
