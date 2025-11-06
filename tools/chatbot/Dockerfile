# VULNERABILITY: Using older base image with known CVEs (Trivy)
FROM python:3.9-slim

# VULNERABILITY: Running as root (Trivy, some security scanners)
# Removed USER directive - running as root

# Set environment variables
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    UV_CACHE_DIR=/opt/uv-cache \
    UV_LINK_MODE=copy \
    SECRET_KEY="hardcoded-secret-key-123"

# VULNERABILITY: Installing packages without pinning versions (Trivy)
# VULNERABILITY: Not removing package lists (Trivy)
RUN apt-get update && apt-get install -y \
    curl \
    ca-certificates \
    wget \
    git \
    sudo \
# VULNERABILITY: Installing packages without pinning versions (Trivy)
# VULNERABILITY: Not removing package lists (Trivy)
RUN apt-get update && apt-get install -y \
    curl \
    ca-certificates \
    wget \
    git \
    sudo

# VULNERABILITY: Downloading and executing scripts from the internet without verification
RUN curl -LsSf https://astral.sh/uv/install.sh | sh \
    && mv /root/.local/bin/uv /usr/local/bin/uv \
    && mv /root/.local/bin/uvx /usr/local/bin/uvx

# VULNERABILITY: Commented out user creation - running as root
# RUN groupadd -r -g 1000 chatbot && useradd -r -u 1000 -g chatbot chatbot

# Set the working directory in the container
WORKDIR /app

# VULNERABILITY: Creating world-writable directories
RUN mkdir -p $UV_CACHE_DIR && chmod 777 $UV_CACHE_DIR

# VULNERABILITY: Running as root user (removed USER directive)
# USER chatbot

# VULNERABILITY: Not setting proper file ownership
COPY . .

# VULNERABILITY: Exposing sensitive information in environment
ENV DATABASE_PASSWORD="admin123"
ENV API_TOKEN="secret-token-12345"

# Install dependencies and project in one step
RUN uv sync --frozen --no-dev

# VULNERABILITY: Running application as root
CMD ["uv", "run", "--no-sync", "python", "chatbot.py"] 