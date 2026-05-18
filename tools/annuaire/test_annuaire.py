import os
import shutil
import tempfile
import unittest
from unittest import mock

import yaml

import annuaire
from annuaire import (
    load_clients,
    build_client_entry,
    API_ENDPOINT,
    HEALTH_ENDPOINT,
    TOPOLOGY_ROOT_KEY,
    TOPOLOGY_CLIENTS_KEY,
)

FIXTURE_PATH = os.path.join(os.path.dirname(__file__), "fixtures", "topology.yaml")
VALUES_PATH_PATCH = "annuaire.VALUES_PATH"


class AnnuaireTestCase(unittest.TestCase):
    def setUp(self):
        self.tempdir = tempfile.TemporaryDirectory()
        self.values_path = os.path.join(self.tempdir.name, "values.yaml")
        shutil.copy(FIXTURE_PATH, self.values_path)

    def tearDown(self):
        self.tempdir.cleanup()

    def test_api(self):
        with mock.patch(VALUES_PATH_PATCH, self.values_path):
            app = annuaire.create_app()
            client = app.test_client()
            response = client.get(API_ENDPOINT)
            self.assertEqual(response.status_code, 200)
            data = response.json
            self.assertIsInstance(data, list)
            self.assertEqual(len(data), 3)
            ids = [entry["client_id"] for entry in data]
            self.assertIn("fr.health.samu750", ids)
            self.assertIn("fr.health.smur", ids)
            self.assertIn("fr.health.fire", ids)
            # full-perimeter client
            samu = next(e for e in data if e["client_id"] == "fr.health.samu750")
            self.assertEqual(samu["P: 15-15"], ["2.1"])
            self.assertEqual(samu["P: 15-nexsis"], ["1.9"])
            self.assertFalse(samu["directCISU"])
            # directCISU client
            fire = next(e for e in data if e["client_id"] == "fr.health.fire")
            self.assertTrue(fire["directCISU"])
            self.assertEqual(fire["P: 15-nexsis"], ["1.9"])
            self.assertNotIn("P: 15-15", fire)

    def test_healthcheck(self):
        with mock.patch(VALUES_PATH_PATCH, self.values_path):
            app = annuaire.create_app()
            client = app.test_client()
            response = client.get(HEALTH_ENDPOINT)
            self.assertEqual(response.status_code, 200)
            self.assertEqual(
                response.json, {"status": "UP", "service": "SAMU Hub Annuaire"}
            )

    def test_load_clients(self):
        clients = load_clients(self.values_path)
        self.assertEqual(len(clients), 3)
        self.assertEqual(clients[0]["client_id"], "fr.health.samu750")
        self.assertEqual(clients[1]["client_id"], "fr.health.smur")
        self.assertEqual(clients[2]["client_id"], "fr.health.fire")

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

    def test_build_client_entry_direct_cisu(self):
        client = {
            "client_id": "fr.health.fire",
            "editor": "NexSIS",
            "directCISU": True,
            "cisuPerimeterVersions": ["1.9"],
        }
        entry = build_client_entry(client)
        self.assertTrue(entry["directCISU"])
        self.assertEqual(entry["P: 15-nexsis"], ["1.9"])

    def test_load_clients_file_not_found(self):
        missing_path = os.path.join(self.tempdir.name, "nonexistent.yaml")
        with self.assertLogs(level="ERROR") as cm:
            with self.assertRaises(FileNotFoundError):
                load_clients(missing_path)
        self.assertTrue(any("not found" in line for line in cm.output))

    def test_load_clients_missing_root_key(self):
        path = os.path.join(self.tempdir.name, "bad.yaml")
        with open(path, "w") as f:
            yaml.dump({"wrong-key": {"clients": []}}, f)
        with self.assertLogs(level="ERROR") as cm:
            with self.assertRaises(RuntimeError) as ctx:
                load_clients(path)
        self.assertIn(TOPOLOGY_ROOT_KEY, str(ctx.exception))
        self.assertTrue(any("Failed to load" in line for line in cm.output))

    def test_load_clients_missing_clients_key(self):
        path = os.path.join(self.tempdir.name, "no_clients.yaml")
        with open(path, "w") as f:
            yaml.dump({TOPOLOGY_ROOT_KEY: {"other-key": []}}, f)
        with self.assertLogs(level="ERROR") as cm:
            with self.assertRaises(RuntimeError) as ctx:
                load_clients(path)
        self.assertIn(f"{TOPOLOGY_ROOT_KEY}.{TOPOLOGY_CLIENTS_KEY}", str(ctx.exception))
        self.assertTrue(any("Failed to load" in line for line in cm.output))

    def test_load_clients_yaml_parse_error(self):
        path = os.path.join(self.tempdir.name, "broken.yaml")
        with open(path, "w") as f:
            f.write("key: [\nunot closed\n")
        with self.assertLogs(level="ERROR") as cm:
            with self.assertRaises(RuntimeError) as ctx:
                load_clients(path)
        self.assertIn("Failed to load clients from", str(ctx.exception))
        self.assertTrue(any("Failed to load" in line for line in cm.output))


if __name__ == "__main__":
    unittest.main()
