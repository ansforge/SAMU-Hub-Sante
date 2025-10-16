import unittest
import logging

from utils import remove_error_keys
from checks.status import Status


class UtilsTestCase(unittest.TestCase):
    def setUp(self):
        # Prevent logging errors when the mocks throw exceptions.
        logging.disable(logging.CRITICAL)

    def tearDown(self):
        logging.disable(logging.NOTSET)

    def test_remove_error_keys(
        self,
    ):
        # Test if error keys are properly removed
        data = {
            "status": Status.UP.value,
            "components": {
                "rabbitmq_server": {"status": Status.UP.value},
                "dispatcher1": {
                    "status": Status.DOWN.value,
                    "error": "Dispatcher failed",
                },
            },
        }
        result = remove_error_keys(data)
        self.assertNotIn("error", result["components"]["dispatcher1"])
        self.assertEqual(result["status"], Status.UP.value)


if __name__ == "__main__":
    unittest.main()
