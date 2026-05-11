import unittest
import tempfile
import os
import yaml
import annuaire
from unittest import mock
from annuaire import (
    load_clients,
    build_client_entry,
    API_ENDPOINT,
    HEALTH_ENDPOINT,
)

YAML_FIXTURE = {
    "hubsante-topology": {
        "clients": [
            {
                "client_id": "fr.health.samu750",
                "editor": "Editeur A",
                "lrmPerimeterVersions": ["2.1"],
                "cisuPerimeterVersions": ["1.9"],
                "directCISU": False,
            },
            {
                "client_id": "fr.health.smur",
                "editor": "Editeur B",
                "smurPerimeterVersions": ["1.7"],
                "directCISU": False,
            },
        ]
    }
}

FOLDER_NAME_PATCH_PATH = "annuaire.VALUES_DIR"
VALUES_DIR_PATCH_PATH = "annuaire.VALUES_DIR"


class AnnuaireTestCase(unittest.TestCase):
    def setUp(self):
        self.tempdir = tempfile.TemporaryDirectory()

    def tearDown(self):
        self.tempdir.cleanup()

    def _write_temp_yaml(self, data):
        path = os.path.join(self.tempdir.name, "values.yaml")
        with open(path, "w") as f:
            yaml.dump(data, f)
        return path

    def test_api(self):
        path = self._write_temp_yaml(YAML_FIXTURE)
        with mock.patch(VALUES_DIR_PATCH_PATH, self.tempdir.name):
            app = annuaire.create_app()
            client = app.test_client()
            response = client.get(API_ENDPOINT)
            self.assertEqual(response.status_code, 200)
            self.assertIsInstance(response.json, list)

    def test_healthcheck(self):
        path = self._write_temp_yaml(YAML_FIXTURE)
        with mock.patch(VALUES_DIR_PATCH_PATH, self.tempdir.name):
            app = annuaire.create_app()
            client = app.test_client()
            response = client.get(HEALTH_ENDPOINT)
            self.assertEqual(response.status_code, 200)
            self.assertEqual(
                response.json, {"status": "UP", "service": "SAMU Hub Annuaire"}
            )

    def test_load_clients(self):
        path = self._write_temp_yaml(YAML_FIXTURE)
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


if __name__ == "__main__":
    unittest.main()
