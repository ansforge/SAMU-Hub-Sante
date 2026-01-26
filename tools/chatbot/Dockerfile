# Builder stage - use standard Python image for building
FROM python:3.13-slim AS builder

# Set environment variables
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    UV_CACHE_DIR=/tmp/uv-cache \
    UV_LINK_MODE=copy

# Install uv
RUN apt-get update && apt-get install -y curl ca-certificates && \
    curl -LsSf https://astral.sh/uv/install.sh | sh && \
    mv /root/.local/bin/uv /usr/local/bin/uv && \
    rm -rf /var/lib/apt/lists/*

# Set the working directory
WORKDIR /app

# Copy dependency files
COPY pyproject.toml uv.lock ./

# Install dependencies
RUN uv sync --frozen --no-dev

# Final stage - use hardened image
FROM dhi.io/python:3.13

# Set environment variables
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

# Set the working directory
WORKDIR /app

# Copy virtual environment from builder
COPY --from=builder /app/.venv /app/.venv

# Copy application code
COPY . .

# Run as non-root user
USER 1000:1000

# Run the application using the virtual environment directly
ENTRYPOINT ["/app/.venv/bin/python"]
CMD ["chatbot.py"]