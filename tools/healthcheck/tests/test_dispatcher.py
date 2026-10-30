import logging
import os
import unittest
from unittest.mock import patch, mock_open, MagicMock

from parameterized import parameterized

from checks.dispatcher import DispatchersHealthcheck
from checks.status import Status


def _instance_without_load() -> DispatchersHealthcheck:
    with patch.object(DispatchersHealthcheck, "_load_config", lambda self: None):
        return DispatchersHealthcheck()


class ParseLineTestCase(unittest.TestCase):
    def setUp(self):
        self.checker = _instance_without_load()

    def test_valid_line_parsed(self):
        self.assertEqual(
            self.checker._parse_line("name;http://host/health"),
            ("name", "http://host/health"),
        )

    def test_empty_and_whitespace_lines_ignored(self):
        self.assertIsNone(self.checker._parse_line(""))
        self.assertIsNone(self.checker._parse_line("   \n"))

    def test_comment_lines_ignored(self):
        self.assertIsNone(self.checker._parse_line("# a comment"))
        self.assertIsNone(self.checker._parse_line("#name;http://host/health"))

    def test_missing_separator_ignored(self):
        self.assertIsNone(self.checker._parse_line("malformed-line"))

    def test_blank_name_ignored(self):
        self.assertIsNone(self.checker._parse_line(";http://host/health"))

    def test_blank_url_ignored(self):
        self.assertIsNone(self.checker._parse_line("name;"))

    def test_url_with_semicolon_preserved(self):
        self.assertEqual(
            self.checker._parse_line("name;http://host/health;token=x"),
            ("name", "http://host/health;token=x"),
        )

    def test_whitespace_trimmed(self):
        self.assertEqual(
            self.checker._parse_line("  name  ;  http://host/health  \n"),
            ("name", "http://host/health"),
        )


class BuildConfigFilePathTestCase(unittest.TestCase):
    def setUp(self):
        self.checker = _instance_without_load()

    def test_absolute_path_returned_unchanged(self):
        with patch("checks.dispatcher.DISPATCHER_CONFIG_FILE_PATH", "/abs/path.txt"):
            self.assertEqual(self.checker._build_config_file_path(), "/abs/path.txt")

    def test_relative_path_resolved_from_healthcheck_dir(self):
        with patch("checks.dispatcher.DISPATCHER_CONFIG_FILE_PATH", "config.txt"):
            path = self.checker._build_config_file_path()
            self.assertTrue(os.path.isabs(path))
            self.assertTrue(path.endswith(os.sep + "config.txt"))


class LoadConfigTestCase(unittest.TestCase):
    def setUp(self):
        self.checker = _instance_without_load()

    def test_raises_when_file_missing(self):
        with patch(
            "checks.dispatcher.DISPATCHER_CONFIG_FILE_PATH", "/nonexistent/path.txt"
        ):
            with self.assertRaises(FileNotFoundError):
                self.checker._load_config()

    def test_raises_when_no_valid_dispatchers(self):
        with (
            patch("checks.dispatcher.DISPATCHER_CONFIG_FILE_PATH", "/abs/path.txt"),
            patch("checks.dispatcher.os.path.exists", return_value=True),
            patch(
                "checks.dispatcher.open",
                mock_open(read_data="# only comments\n\n"),
            ),
        ):
            with self.assertRaises(RuntimeError):
                self.checker._load_config()

    def test_loads_valid_file(self):
        config_content = (
            "# comment\n"
            "dispatcher1;http://dispatcher1/health\n"
            "dispatcher2;http://dispatcher2/health\n"
        )
        with (
            patch("checks.dispatcher.DISPATCHER_CONFIG_FILE_PATH", "/abs/path.txt"),
            patch("checks.dispatcher.os.path.exists", return_value=True),
            patch("checks.dispatcher.open", mock_open(read_data=config_content)),
        ):
            self.checker._load_config()

        self.assertEqual(
            self.checker.dispatcher_instances,
            {
                "dispatcher1": "http://dispatcher1/health",
                "dispatcher2": "http://dispatcher2/health",
            },
        )


class PerformChecksTestCase(unittest.TestCase):
    def setUp(self):
        logging.disable(logging.CRITICAL)
        self.checker = _instance_without_load()
        self.checker.dispatcher_instances = {
            "dispatcher1": "http://dispatcher1/health",
            "dispatcher2": "http://dispatcher2/health",
        }

    def tearDown(self):
        logging.disable(logging.NOTSET)

    def _mock_responses(self, statuses):
        def side_effect(url, **kwargs):
            name = url.split("//")[1].split("/")[0]
            response = MagicMock()
            response.json.return_value = {"status": statuses[name], "components": {}}
            return response

        return side_effect

    @parameterized.expand(
        [
            ("all_up", "UP", "UP", Status.UP.value),
            ("all_down", "DOWN", "DOWN", Status.DOWN.value),
            ("one_down", "UP", "DOWN", Status.DEGRADED.value),
            ("one_degraded", "UP", "DEGRADED", Status.DEGRADED.value),
            ("all_degraded", "DEGRADED", "DEGRADED", Status.DEGRADED.value),
        ]
    )
    @patch("checks.dispatcher.http_session.get")
    def test_aggregated_status(self, _, status1, status2, expected, mock_get):
        mock_get.side_effect = self._mock_responses(
            {"dispatcher1": status1, "dispatcher2": status2}
        )
        result = self.checker.check_wrapper()
        self.assertEqual(result["dispatchers"]["status"], expected)
        self.assertEqual(result["dispatcher1"]["status"], status1)
        self.assertEqual(result["dispatcher2"]["status"], status2)

    @patch("checks.dispatcher.http_session.get")
    def test_unexpected_actuator_status_is_down(self, mock_get):
        mock_get.side_effect = self._mock_responses(
            {"dispatcher1": "OUT_OF_SERVICE", "dispatcher2": "UP"}
        )
        result = self.checker.check_wrapper()
        self.assertEqual(result["dispatcher1"]["status"], Status.DOWN.value)
        self.assertEqual(result["dispatchers"]["status"], Status.DEGRADED.value)

    @patch("checks.dispatcher.http_session.get")
    def test_degraded_instance_metric_is_zero(self, mock_get):
        mock_get.side_effect = self._mock_responses(
            {"dispatcher1": "DEGRADED", "dispatcher2": "UP"}
        )
        self.checker.check_wrapper()
        metric = DispatchersHealthcheck.dispatcher_status_metric
        self.assertEqual(metric.labels(dispatcher="dispatcher1")._value.get(), 0)
        self.assertEqual(metric.labels(dispatcher="dispatcher2")._value.get(), 1)

    @patch("checks.dispatcher.http_session.get")
    def test_instance_error_is_down_and_aggregate_degraded(self, mock_get):
        def side_effect(url, **kwargs):
            if "dispatcher1" in url:
                raise Exception("unreachable")
            response = MagicMock()
            response.json.return_value = {"status": "UP"}
            return response

        mock_get.side_effect = side_effect
        result = self.checker.check_wrapper()
        self.assertEqual(result["dispatcher1"]["status"], Status.DOWN.value)
        self.assertEqual(result["dispatchers"]["status"], Status.DEGRADED.value)

    def test_fallback_marks_all_down(self):
        result = self.checker.check_failure_fallback()
        self.assertEqual(
            result,
            {
                "dispatcher1": {"status": Status.DOWN.value},
                "dispatcher2": {"status": Status.DOWN.value},
                "dispatchers": {"status": Status.DOWN.value},
            },
        )


if __name__ == "__main__":
    unittest.main()
