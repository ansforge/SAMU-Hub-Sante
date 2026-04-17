import os
import sys

# Required environment variables
REQUIRED_ENV_VARS = [
    "RABBITMQ_URL",
    "RABBITMQ_MONITORING_USERNAME",
    "RABBITMQ_MONITORING_PASSWORD",
    "DISPATCHER_INSTANCES",
]

# Check all required environment variables
missing_vars = [var for var in REQUIRED_ENV_VARS if not os.getenv(var)]
if missing_vars:
    sys.exit(
        f"Error: The following environment variables are not set: {', '.join(missing_vars)}"
    )

HTTP_TIMEOUT = int(
    os.getenv("HTTP_TIMEOUT", 5)
)  # Timeout in seconds, configurable via environment variable
RABBITMQ_URL = os.getenv("RABBITMQ_URL")
RABBITMQ_MONITORING_USERNAME = os.getenv("RABBITMQ_MONITORING_USERNAME")
RABBITMQ_MONITORING_PASSWORD = os.getenv("RABBITMQ_MONITORING_PASSWORD")
RABBITMQ_CA_CERT_PATH = "/etc/ssl/certs/hubsante-rabbitmq-ca.crt"

MONGODB_URI = os.getenv("MONGODB_URI", "mongodb://localhost:27017")
