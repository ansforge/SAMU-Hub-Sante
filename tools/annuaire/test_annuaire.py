import unittest
import tempfile
import os
import annuaire
from unittest import mock
from annuaire import (
    parse_csv,
    select_columns,
    API_ENDPOINT,
    HEALTH_ENDPOINT,
    CSV_FILENAME,
    HEADERS_COLUMNS_TO_KEEP,
    CSV_NOT_FOUND_MSG,
)

FOLDER_NAME_PATCH_PATH = "annuaire.CSV_DIR"


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
                    "1.5,2.0,2.1",
                    "1.7",
                    "1.9",
                    "1.3",
                    "",
                    "",
                    "",
                    "true",
                ],
                [
                    "fr.health.test.samuC",
                    "fr.health.test.samuC",
                    "ANS",
                    "1.5,2.0,2.1",
                    "1.7",
                    "1.9",
                    "1.3",
                    "",
                    "true",
                    "",
                    "true",
                ],
                [
                    "fr.health.test.samuv1",
                    "fr.health.test.samuv1",
                    "ANS",
                    "1.5",
                    "",
                    "",
                    "",
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
        with self.assertLogs(level="ERROR") as cm:
            with self.assertRaises(FileNotFoundError):
                parse_csv("non_existent.csv")
        self.assertIn(CSV_NOT_FOUND_MSG, cm.output[0])

    def test_api(self):
        with mock.patch(FOLDER_NAME_PATCH_PATH, self.tempdir.name):
            app = annuaire.create_app()
            client = app.test_client()
            response = client.get(API_ENDPOINT)
            self.assertEqual(response.status_code, 200)
            self.assertIsInstance(response.json, list)

    def test_parse_csv_valid(self):
        with mock.patch(FOLDER_NAME_PATCH_PATH, self.tempdir.name):
            data = parse_csv(CSV_FILENAME)

            expected_rows = [
                {
                    "client_id": "fr.health.lrm",
                    "editor": "ANS",
                    "P: 15-15": "1.5,2.0,2.1",
                    "P: 15-smur": "1.7",
                    "P: 15-nexsis": "1.9",
                    "P: 15-gps": "1.3",
                },
                {
                    "client_id": "fr.health.test.samuC",
                    "editor": "ANS",
                    "P: 15-15": "1.5,2.0,2.1",
                    "P: 15-smur": "1.7",
                    "P: 15-nexsis": "1.9",
                    "P: 15-gps": "1.3",
                },
                {
                    "client_id": "fr.health.test.samuv1",
                    "editor": "ANS",
                    "P: 15-15": "1.5",
                    "P: 15-smur": "",
                    "P: 15-nexsis": "",
                    "P: 15-gps": "",
                },
            ]

        for i, expected_row in enumerate(expected_rows):
            for key, expected_value in expected_row.items():
                actual_value = data[i].get(key)
                self.assertEqual(
                    actual_value,
                    expected_value,
                    f"Erreur sur la ligne {i + 1}, colonne '{key}': "
                    f"attendu '{expected_value}', obtenu '{actual_value}'",
                )

    def test_select_columns(self):
        with mock.patch(FOLDER_NAME_PATCH_PATH, self.tempdir.name):
            data = parse_csv(CSV_FILENAME)
            result = select_columns(data)
            for row in result:
                self.assertEqual(set(row.keys()), set(HEADERS_COLUMNS_TO_KEEP))

    def test_healthcheck(self):
        with mock.patch(FOLDER_NAME_PATCH_PATH, self.tempdir.name):
            app = annuaire.create_app()
            client = app.test_client()
            response = client.get(HEALTH_ENDPOINT)
            self.assertEqual(response.status_code, 200)
            self.assertEqual(
                response.json, {"status": "UP", "service": "SAMU Hub Annuaire"}
            )


if __name__ == "__main__":
    unittest.main()
