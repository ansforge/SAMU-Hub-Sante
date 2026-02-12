# Builder stage - use Debian Python image for building (onnxruntime needs glibc/manylinux)
FROM dhi.io/python:3.13-dev AS builder

COPY --from=ghcr.io/astral-sh/uv:latest /uv /uvx /bin/

# Set environment variables
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    UV_CACHE_DIR=/tmp/uv-cache \
    UV_LINK_MODE=copy

# Set the working directory
WORKDIR /app

# Copy dependency files
COPY pyproject.toml uv.lock ./

# Install dependencies
RUN uv sync --frozen --no-dev

# Final stage - use standard Debian slim (DHI too minimal for venv)
FROM dhi.io/python:3.13

# Set environment variables
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PATH="/app/.venv/bin:$PATH"

# Set the working directory
WORKDIR /app

# Copy virtual environment from builder
COPY --from=builder --chown=1000:1000 /app/.venv /app/.venv

# Copy application code
COPY chatbot.py chatbot.py

# Explicit non-root user (already set by base image)
USER 1000

# Run the application
CMD ["python", "chatbot.py"]