import logging
import os

from dotenv import load_dotenv

load_dotenv()

DEV_SECRET_KEY = "dev_secret_key_to_change"


class Config:
    CLIENT_URL = os.getenv("CLIENT_URL", "http://localhost:5173")
    BACKEND_URL = os.getenv("BACKEND_URL", "http://localhost:8080")
    GITHUB_CLIENT_ID = os.getenv("GITHUB_CLIENT_ID")
    GITHUB_CLIENT_SECRET = os.getenv("GITHUB_CLIENT_SECRET")
    GITHUB_TOKEN = os.getenv("GITHUB_TOKEN")
    REPO_OWNER = os.getenv("REPO_OWNER", "ansforge")
    REPO_NAME = os.getenv("REPO_NAME", "SAMU-Hub-Modeles")
    SECRET_KEY = os.getenv("FLASK_SECRET_KEY", DEV_SECRET_KEY)
    COOKIE_SECURE = os.getenv("COOKIE_SECURE", "False").lower() in ("true", "1")


if Config.SECRET_KEY == DEV_SECRET_KEY:
    logging.getLogger(__name__).warning(
        "FLASK_SECRET_KEY not set; using insecure development default"
    )
