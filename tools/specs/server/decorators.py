from functools import wraps

from flask import g, jsonify, request

from services.user_repository_service import UserRepositoryService


def auth_required(f):
    @wraps(f)
    def decorated_function(*args, **kwargs):
        token = request.cookies.get("gh_token")
        if not token:
            return jsonify({"error": "Not authenticated."}), 401

        g.github_service = UserRepositoryService(token=token)
        return f(*args, **kwargs)

    return decorated_function
