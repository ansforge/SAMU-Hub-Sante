import os
import tempfile
import unittest
from unittest import mock

import yaml

import annuaire
from annuaire import (
    load_clients,
    resolve_perimeters,
    build_annuaire_clients,
    CLIENTS_ENDPOINT,
    HEALTH_ENDPOINT,
    ANNUAIRE_ROOT_KEY,
    ANNUAIRE_CLIENTS_KEY,
)

FIXTURE_DIR = os.path.join(os.path.dirname(__file__), "fixtures")
FIXTURE_PATH = os.path.join(FIXTURE_DIR, "values_annuaire_api.yaml")
VALUES_PATH_PATCH = "annuaire.VALUES_PATH"


class AnnuaireTestCase(unittest.TestCase):
    def setUp(self):
        self.tempdir = tempfile.TemporaryDirectory()

    def tearDown(self):
        self.tempdir.cleanup()

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
        self.assertEqual(len(clients), 3)
        self.assertEqual(clients[0]["client_id"], "fr.health.samu750")
        self.assertEqual(clients[1]["client_id"], "fr.fire.sdis750")
        self.assertEqual(clients[2]["client_id"], "fr.health.fire")

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
        self.assertIn(ANNUAIRE_ROOT_KEY, str(ctx.exception))
        self.assertTrue(any("Failed to load" in line for line in cm.output))

    def test_load_clients_missing_clients_key(self):
        path = os.path.join(self.tempdir.name, "no_clients.yaml")
        with open(path, "w") as f:
            yaml.dump({ANNUAIRE_ROOT_KEY: {"other-key": []}}, f)
        with self.assertLogs(level="ERROR") as cm:
            with self.assertRaises(RuntimeError) as ctx:
                load_clients(path)
        self.assertIn(f"{ANNUAIRE_ROOT_KEY}.{ANNUAIRE_CLIENTS_KEY}", str(ctx.exception))
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

    def test_resolve_perimeters_missing_topology_key(self):
        client = {
            "client_id": "fr.health.cap-only",
            "annuaire": {"lrm": True, "cap": True},
            "editor": "ANS",
        }
        perimeters = resolve_perimeters(client.get("annuaire"))
        self.assertEqual(
            perimeters.model_dump(by_alias=True),
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
        client = result[0].model_dump(by_alias=True)
        self.assertEqual(client.get("client_id"), "fr.health.samu750")
        self.assertEqual(
            client.get("perimeters"),
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


class AnnuaireClientsApiTestCase(unittest.TestCase):
    def setUp(self):
        self.patcher = mock.patch(VALUES_PATH_PATCH, FIXTURE_PATH)
        self.patcher.start()
        self.http = annuaire.create_app().test_client()

    def tearDown(self):
        self.patcher.stop()

    def test_clients_without_annuaire_key_are_excluded(self):
        response = self.http.get(CLIENTS_ENDPOINT)
        self.assertEqual(response.status_code, 200)
        ids = [entry["client_id"] for entry in response.json]
        self.assertNotIn("fr.health.fire", ids)

    def test_clients_api_returns_only_clients_with_annuaire(self):
        response = self.http.get(CLIENTS_ENDPOINT)
        self.assertEqual(response.status_code, 200)
        ids = [entry["client_id"] for entry in response.json]
        self.assertEqual(ids, ["fr.health.samu750", "fr.fire.sdis750"])

    def test_clients_api_response_shape(self):
        data = self.http.get(CLIENTS_ENDPOINT).json
        samu = next(
            entry for entry in data if entry["client_id"] == "fr.health.samu750"
        )
        self.assertEqual(samu["client_name"], "SAMU 750")
        self.assertEqual(samu["client_type"], "SAMU")
        self.assertNotIn("editor", samu)
        self.assertNotIn("directCISU", samu)
        self.assertNotIn("isLinkedToNexsis", samu)
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

    def test_clients_api_exposes_false_for_not_implemented_perimeters(self):
        data = self.http.get(CLIENTS_ENDPOINT).json
        sdis = next(entry for entry in data if entry["client_id"] == "fr.fire.sdis750")
        self.assertFalse(sdis["perimeters"]["15-15"])
        self.assertFalse(sdis["perimeters"]["15-cap"])

    def test_clients_api_filter_by_perimeter(self):
        response_1515 = self.http.get(f"{CLIENTS_ENDPOINT}/15-15")
        self.assertEqual(response_1515.status_code, 200)
        self.assertEqual(len(response_1515.json), 1)
        self.assertEqual(response_1515.json[0]["client_id"], "fr.health.samu750")

        response_cap = self.http.get(f"{CLIENTS_ENDPOINT}/15-cap")
        self.assertEqual(response_cap.status_code, 200)
        self.assertEqual(len(response_cap.json), 1)
        self.assertEqual(response_cap.json[0]["client_id"], "fr.health.samu750")

        response_nexsis = self.http.get(f"{CLIENTS_ENDPOINT}/15-nexsis")
        self.assertEqual(response_nexsis.status_code, 200)
        self.assertEqual(len(response_nexsis.json), 2)

        response_unknown = self.http.get(f"{CLIENTS_ENDPOINT}/inconnu")
        self.assertEqual(response_unknown.status_code, 400)
        self.assertIn("error", response_unknown.json)
        self.assertIn("valid_perimeters", response_unknown.json)
        self.assertIsInstance(response_unknown.json["valid_perimeters"], list)

    def test_clients_filter_rejects_all_unknown_perimeters(self):
        for invalid in ["unknown", "P: 15-15", "injection-attempt"]:
            response = self.http.get(f"{CLIENTS_ENDPOINT}/{invalid}")
            self.assertEqual(
                response.status_code,
                400,
                msg=f"Expected 400 for perimeter '{invalid}'",
            )


if __name__ == "__main__":
    unittest.main()
