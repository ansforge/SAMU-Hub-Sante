from functools import wraps

from flask import g, jsonify, request

from services.user_account import UserAccount


def auth_required(f):
    @wraps(f)
    def decorated_function(*args, **kwargs):
        token = request.cookies.get("gh_token")
        if not token:
            return jsonify({"error": "Not authenticated."}), 401

        g.github_service = UserAccount(token=token)
        return f(*args, **kwargs)

    return decorated_function
