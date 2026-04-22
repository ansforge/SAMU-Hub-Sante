import os
import unittest
from unittest.mock import patch, mock_open

from checks.dispatcher import DispatchersHealthcheck


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


if __name__ == "__main__":
    unittest.main()
