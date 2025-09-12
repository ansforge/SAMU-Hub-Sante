import unittest
import tempfile
import os
import annuaire
from unittest import mock
from annuaire import (
    parse_csv,
    select_columns,
    CSV_DATA_KEY,
    API_ENDPOINT,
    CSV_FILENAME,
    HEADERS_COLUMNS_TO_KEEP,
)
from flask import Flask


class AnnuaireTestCase(unittest.TestCase):

    def setUp(self):
        self.tempdir = tempfile.TemporaryDirectory()
        self.csv_path = os.path.join(self.tempdir.name, CSV_FILENAME)
        self._write_temp_csv(
            [
                [
                    "client_id",
                    "CommonName",
                    "editor",
                    "P: 15-15",
                    "P: 15-smur",
                    "P: 15-nexsis",
                    "P: 15-gps",
                    "useXML",
                    "directCISU",
                    "additionalPermissions",
                    "lrm_test",
                ],
                [
                    "fr.health.lrm",
                    "lrm.messaging.bac-a-sable.hub.esante.gouv.fr",
                    "ANS",
                    "1.5;2.0;2.1",
                    "1.6;1.7",
                    "1.9",
                    "1.3",
                    "",
                    "",
                    "",
                    "true",
                ],
            ]
        )

    def tearDown(self):
        self.tempdir.cleanup()

    def _write_temp_csv(self, rows):
        with open(self.csv_path, "w", encoding="utf-8", newline="") as f:
            for row in rows:
                f.write(";".join(row) + "\n")

    def test_parse_csv_file_not_found(self):
        with self.assertRaises(FileNotFoundError):
            parse_csv("non_existent.csv")

    def test_api(self):
        with mock.patch("annuaire.CSV_DIR", self.tempdir.name):
            app = annuaire.create_app()

            @app.get(API_ENDPOINT)
            def get_json():
                return app.response_class(
                    app.json.dumps(app.config[CSV_DATA_KEY]),
                    mimetype="application/json",
                )

            client = app.test_client()
            response = client.get(API_ENDPOINT)
            self.assertEqual(response.status_code, 200)
            self.assertIsInstance(response.json, list)

    def test_parse_csv_valid(self):
        with mock.patch("annuaire.CSV_DIR", self.tempdir.name):
            data = parse_csv(CSV_FILENAME)
            self.assertEqual(len(data), 1)
            self.assertEqual(data[0][HEADERS_COLUMNS_TO_KEEP[0]], "fr.health.lrm")

    def test_select_columns(self):
        with mock.patch("annuaire.CSV_DIR", self.tempdir.name):
            data = parse_csv(CSV_FILENAME)
            result = select_columns(data)
            for row in result:
                self.assertIn(HEADERS_COLUMNS_TO_KEEP[0], row)
                self.assertIn(HEADERS_COLUMNS_TO_KEEP[1], row)
                self.assertNotIn("CommonName", row)


if __name__ == "__main__":
    unittest.main()
