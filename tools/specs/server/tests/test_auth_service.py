from unittest.mock import patch

from services.auth_service import auth_service
from specs import create_app

app = create_app()


@patch("services.auth_service.Github")
def test_verify_collaborator_permissions_true_for_write(mock_github_cls):
    mock_gh = mock_github_cls.return_value
    mock_gh.get_user.return_value.login = "alice"
    mock_gh.get_repo.return_value.get_collaborator_permission.return_value = "write"
    assert auth_service.verify_collaborator_permissions("token") is True


@patch("services.auth_service.Github")
def test_verify_collaborator_permissions_false_for_read(mock_github_cls):
    mock_gh = mock_github_cls.return_value
    mock_gh.get_user.return_value.login = "alice"
    mock_gh.get_repo.return_value.get_collaborator_permission.return_value = "read"
    assert auth_service.verify_collaborator_permissions("token") is False


def test_create_login_response_sets_cookie_and_redirects():
    with app.app_context():
        res = auth_service.create_login_response("gh_access_token")
    assert res.status_code == 302
    assert "gh_token=gh_access_token" in res.headers["Set-Cookie"]
    assert "HttpOnly" in res.headers["Set-Cookie"]


def test_create_logout_response_clears_cookie():
    with app.app_context():
        res = auth_service.create_logout_response()
    assert res.get_json() == {"status": "logged_out"}
    assert (
        'gh_token=""' in res.headers["Set-Cookie"]
        or "gh_token=;" in res.headers["Set-Cookie"]
    )
