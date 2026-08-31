from flask import Response, jsonify, make_response, redirect
from github import GithubException

from config import Config
from services.service_account import ServiceAccount
from services.user_account import UserAccount

ALLOWED_PERMISSIONS = ["write", "admin"]


class AuthService:
    def __init__(self):
        self.github_service = ServiceAccount()

    def verify_collaborator_permissions(self, access_token: str) -> bool:
        user_account = UserAccount(token=access_token)
        try:
            username = user_account.get_me()["login"]
            permission = self.github_service.get_collaborator_permission(username)
        except GithubException:
            return False
        finally:
            user_account.close()

        return permission in ALLOWED_PERMISSIONS

    def create_login_response(self, access_token: str) -> Response:
        response = make_response(redirect(Config.CLIENT_URL))
        response.set_cookie(
            "gh_token",
            access_token,
            httponly=True,
            secure=Config.COOKIE_SECURE,
            samesite="Lax",
            max_age=3600 * 8,  # 8 heures
        )
        return response

    def create_logout_response(self) -> Response:
        response = make_response(jsonify({"status": "logged_out"}))
        response.delete_cookie("gh_token")
        return response


auth_service = AuthService()
