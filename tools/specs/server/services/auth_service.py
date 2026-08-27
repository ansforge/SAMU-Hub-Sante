from flask import Response, jsonify, make_response, redirect
from github import Github, GithubException

from config import Config


class AuthService:
    def verify_collaborator_permissions(self, access_token: str) -> bool:
        gh = Github(access_token)
        try:
            username = gh.get_user().login
            repo = gh.get_repo(f"{Config.REPO_OWNER}/{Config.REPO_NAME}")
            permission = repo.get_collaborator_permission(username)
        except GithubException:
            return False

        return permission in ["write", "admin"]

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
