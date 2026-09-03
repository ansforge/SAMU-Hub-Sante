from specs import create_app


def _client():
    return create_app().test_client()


def test_health():
    res = _client().get("/health")
    assert res.status_code == 200
    assert res.get_json()["status"] == "UP"
