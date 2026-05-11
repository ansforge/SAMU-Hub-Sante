import unittest
import tempfile
import shutil
import os
import annuaire
from unittest import mock
from annuaire import (
    load_clients,
    build_client_entry,
    API_ENDPOINT,
    HEALTH_ENDPOINT,
)

FIXTURE_PATH = os.path.join(os.path.dirname(__file__), "fixtures", "topology.yaml")
VALUES_DIR_PATCH_PATH = "annuaire.VALUES_DIR"


class AnnuaireTestCase(unittest.TestCase):
    def setUp(self):
        self.tempdir = tempfile.TemporaryDirectory()
        shutil.copy(FIXTURE_PATH, os.path.join(self.tempdir.name, "values.yaml"))

    def tearDown(self):
        self.tempdir.cleanup()

    def test_api(self):
        with mock.patch(VALUES_DIR_PATCH_PATH, self.tempdir.name):
            app = annuaire.create_app()
            client = app.test_client()
            response = client.get(API_ENDPOINT)
            self.assertEqual(response.status_code, 200)
            self.assertIsInstance(response.json, list)

    def test_healthcheck(self):
        with mock.patch(VALUES_DIR_PATCH_PATH, self.tempdir.name):
            app = annuaire.create_app()
            client = app.test_client()
            response = client.get(HEALTH_ENDPOINT)
            self.assertEqual(response.status_code, 200)
            self.assertEqual(
                response.json, {"status": "UP", "service": "SAMU Hub Annuaire"}
            )

    def test_load_clients(self):
        path = os.path.join(self.tempdir.name, "values.yaml")
        clients = load_clients(path)
        self.assertEqual(len(clients), 2)
        self.assertEqual(clients[0]["client_id"], "fr.health.samu750")
        self.assertEqual(clients[1]["client_id"], "fr.health.smur")

    def test_build_client_entry_with_lrm(self):
        client = {
            "client_id": "fr.health.samu750",
            "editor": "Editeur A",
            "lrmPerimeterVersions": ["2.1"],
            "directCISU": False,
        }
        entry = build_client_entry(client)
        self.assertEqual(entry["P: 15-15"], ["2.1"])
        self.assertNotIn("P: 15-smur", entry)
        self.assertNotIn("P: 15-nexsis", entry)

    def test_build_client_entry_without_smur(self):
        client = {
            "client_id": "fr.health.samu750",
            "editor": "Editeur A",
            "lrmPerimeterVersions": ["2.1"],
            "directCISU": False,
        }
        entry = build_client_entry(client)
        self.assertNotIn("P: 15-smur", entry)

    def test_load_clients_file_not_found(self):
        with self.assertLogs(level="ERROR") as cm:
            with self.assertRaises(FileNotFoundError):
                load_clients("/nonexistent/path/values.yaml")
        self.assertTrue(any("not found" in line for line in cm.output))


if __name__ == "__main__":
    unittest.main()
