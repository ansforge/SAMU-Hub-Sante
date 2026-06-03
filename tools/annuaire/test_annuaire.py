import os
import tempfile
import unittest
from unittest import mock

import yaml

import annuaire
from annuaire import (
    load_clients,
    build_client_entry,
    resolve_perimeters,
    build_annuaire_clients,
    API_ENDPOINT,
    CLIENTS_ENDPOINT,
    HEALTH_ENDPOINT,
    TOPOLOGY_ROOT_KEY,
    TOPOLOGY_CLIENTS_KEY,
)

FIXTURE_DIR = os.path.join(os.path.dirname(__file__), "fixtures")
FIXTURE_PATH = os.path.join(FIXTURE_DIR, "values.yaml")
ANNULAIRE_API_FIXTURE_PATH = os.path.join(FIXTURE_DIR, "values_annuaire_api.yaml")
VALUES_PATH_PATCH = "annuaire.VALUES_PATH"


class AnnuaireTestCase(unittest.TestCase):
    def setUp(self):
        self.tempdir = tempfile.TemporaryDirectory()

    def tearDown(self):
        self.tempdir.cleanup()

    def test_api(self):
        with mock.patch(VALUES_PATH_PATCH, FIXTURE_PATH):
            app = annuaire.create_app()
            client = app.test_client()
            response = client.get(API_ENDPOINT)
            self.assertEqual(response.status_code, 200)
            data = response.json
            self.assertIsInstance(data, list)
            self.assertEqual(len(data), 4)
            ids = [entry["client_id"] for entry in data]
            self.assertIn("fr.health.samu750", ids)
            self.assertIn("fr.health.test.samuv1", ids)
            self.assertIn("fr.health.fire", ids)
            self.assertIn("fr.health.test.samuC", ids)
            # full-perimeter client
            samu = next(e for e in data if e["client_id"] == "fr.health.samu750")
            self.assertEqual(samu["P: 15-15"], ["2.1"])
            self.assertEqual(samu["P: 15-nexsis"], ["1.9"])
            self.assertFalse(samu["directCISU"])
            # directCISU client (CISU only)
            fire = next(e for e in data if e["client_id"] == "fr.health.fire")
            self.assertTrue(fire["directCISU"])
            self.assertEqual(fire["P: 15-nexsis"], ["1.9"])
            self.assertNotIn("P: 15-15", fire)
            # full-perimeter client with directCISU
            samuC = next(e for e in data if e["client_id"] == "fr.health.test.samuC")
            self.assertTrue(samuC["directCISU"])
            self.assertEqual(samuC["P: 15-15"], ["1.5", "2.0", "2.1"])
            self.assertEqual(samuC["P: 15-nexsis"], ["1.9"])

    def test_healthcheck(self):
        with mock.patch(VALUES_PATH_PATCH, FIXTURE_PATH):
            app = annuaire.create_app()
            client = app.test_client()
            response = client.get(HEALTH_ENDPOINT)
            self.assertEqual(response.status_code, 200)
            self.assertEqual(
                response.json, {"status": "UP", "service": "SAMU Hub Annuaire"}
            )

    def test_load_clients(self):
        clients = load_clients(FIXTURE_PATH)
        self.assertEqual(len(clients), 4)
        self.assertEqual(clients[0]["client_id"], "fr.health.samu750")
        self.assertEqual(clients[1]["client_id"], "fr.health.test.samuv1")
        self.assertEqual(clients[2]["client_id"], "fr.health.fire")
        self.assertEqual(clients[3]["client_id"], "fr.health.test.samuC")

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

    def test_clients_api_returns_only_clients_with_annuaire(self):
        with mock.patch(VALUES_PATH_PATCH, ANNULAIRE_API_FIXTURE_PATH):
            app = annuaire.create_app()
            client = app.test_client()
            response = client.get(CLIENTS_ENDPOINT)
            self.assertEqual(response.status_code, 200)
            data = response.json

            self.assertEqual(len(data), 2)
            ids = [entry["client_id"] for entry in data]
            self.assertEqual(ids, ["fr.health.samu750", "fr.fire.sdis750"])

            samu = next(entry for entry in data if entry["client_id"] == "fr.health.samu750")
            self.assertEqual(samu["client_name"], "SAMU 750")
            self.assertEqual(samu["client_type"], "SAMU")
            self.assertFalse(samu["directCISU"])
            self.assertFalse(samu["isLinkedToNexsis"])
            self.assertEqual(
                samu["perimeters"],
                {
                    "15-15": True,
                    "15-cap": True,
                    "15-portail": False,
                    "15-cnr114": False,
                    "15-nexsis": True,
                    "15-smur": False,
                    "15-gps": False,
                },
            )

            sdis = next(entry for entry in data if entry["client_id"] == "fr.fire.sdis750")
            self.assertEqual(
                sdis["perimeters"],
                {
                    "15-15": False,
                    "15-cap": False,
                    "15-portail": False,
                    "15-cnr114": False,
                    "15-nexsis": True,
                    "15-smur": False,
                    "15-gps": False,
                },
            )
            self.assertTrue(sdis["directCISU"])
            self.assertTrue(sdis["isLinkedToNexsis"])

    def test_clients_api_exposes_false_for_not_implemented_perimeters(self):
        with mock.patch(VALUES_PATH_PATCH, ANNULAIRE_API_FIXTURE_PATH):
            app = annuaire.create_app()
            client = app.test_client()
            response = client.get(CLIENTS_ENDPOINT)
            self.assertEqual(response.status_code, 200)
            data = response.json

            sdis = next(entry for entry in data if entry["client_id"] == "fr.fire.sdis750")
            self.assertFalse(sdis["perimeters"]["15-15"])
            self.assertFalse(sdis["perimeters"]["15-cap"])

    def test_clients_api_filter_by_perimeter(self):
        with mock.patch(VALUES_PATH_PATCH, ANNULAIRE_API_FIXTURE_PATH):
            app = annuaire.create_app()
            client = app.test_client()

            response_1515 = client.get(f"{CLIENTS_ENDPOINT}/15-15")
            self.assertEqual(response_1515.status_code, 200)
            self.assertEqual(len(response_1515.json), 1)
            self.assertEqual(response_1515.json[0]["client_id"], "fr.health.samu750")

            response_cap = client.get(f"{CLIENTS_ENDPOINT}/15-cap")
            self.assertEqual(response_cap.status_code, 200)
            self.assertEqual(len(response_cap.json), 1)
            self.assertEqual(response_cap.json[0]["client_id"], "fr.health.samu750")

            response_nexsis = client.get(f"{CLIENTS_ENDPOINT}/15-nexsis")
            self.assertEqual(response_nexsis.status_code, 200)
            self.assertEqual(len(response_nexsis.json), 2)

            response_unknown = client.get(f"{CLIENTS_ENDPOINT}/inconnu")
            self.assertEqual(response_unknown.status_code, 400)
            self.assertIn("error", response_unknown.json)

    def test_clients_filter_rejects_all_unknown_perimeters(self):
        with mock.patch(VALUES_PATH_PATCH, ANNULAIRE_API_FIXTURE_PATH):
            app = annuaire.create_app()
            client = app.test_client()
            for invalid in ["unknown", "P: 15-15", "injection-attempt"]:
                response = client.get(f"{CLIENTS_ENDPOINT}/{invalid}")
                self.assertEqual(
                    response.status_code,
                    400,
                    msg=f"Expected 400 for perimeter '{invalid}'",
                )

    def test_resolve_perimeters_missing_topology_key(self):
        client = {
            "client_id": "fr.health.cap-only",
            "annuaire": {"lrm": True, "cap": True},
            "editor": "ANS",
        }
        perimeters = resolve_perimeters(client)
        self.assertEqual(
            perimeters,
            {
                "15-15": True,
                "15-cap": True,
                "15-portail": False,
                "15-cnr114": False,
                "15-nexsis": False,
                "15-smur": False,
                "15-gps": False,
            },
        )

    def test_build_annuaire_clients(self):
        clients = [
            {
                "client_id": "fr.health.samu750",
                "client_name": "SAMU 750",
                "client_type": "SAMU",
                "editor": "Editeur A",
                "lrmPerimeterVersions": ["2.1"],
                "annuaire": {"lrm": True},
            },
            {
                "client_id": "fr.health.ignore",
                "editor": "ANS",
            },
        ]

        result = build_annuaire_clients(clients)
        self.assertEqual(len(result), 1)
        self.assertEqual(result[0]["client_id"], "fr.health.samu750")
        self.assertEqual(
            result[0]["perimeters"],
            {
                "15-15": True,
                "15-cap": False,
                "15-portail": False,
                "15-cnr114": False,
                "15-nexsis": False,
                "15-smur": False,
                "15-gps": False,
            },
        )


if __name__ == "__main__":
    unittest.main()
