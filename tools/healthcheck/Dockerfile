# Use an official Python runtime as a parent image
FROM python:3.14-alpine

# Set environment variables
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    UV_CACHE_DIR=/opt/uv-cache \
    UV_LINK_MODE=copy

# Install system dependencies and uv
RUN apk add --no-cache \
    curl \
    ca-certificates \
    && curl -LsSf https://astral.sh/uv/install.sh | sh \
    && mv /root/.local/bin/uv /usr/local/bin/uv \
    && mv /root/.local/bin/uvx /usr/local/bin/uvx

# Create a non-root user and group
RUN addgroup -g 1000 healthcheck && adduser -D -u 1000 -G healthcheck healthcheck

# Set the working directory in the container
WORKDIR /app

# Create UV cache directory with proper permissions and set ownership of working directory
RUN mkdir -p $UV_CACHE_DIR && \
    chown -R healthcheck:healthcheck $UV_CACHE_DIR && \
    chown -R healthcheck:healthcheck /app

# Switch to the non-root user
USER healthcheck

# Copy dependency files first for better layer caching
COPY --chown=healthcheck:healthcheck pyproject.toml uv.lock ./

# Install dependencies
RUN uv sync --frozen --no-dev

# Copy the current directory contents into the container at /app
COPY --chown=healthcheck:healthcheck . .

# Make port 8080 available to the world outside this container
EXPOSE 8080

# Use Gunicorn to serve the Flask app
CMD ["uv", "run", "gunicorn", "-w", "4", "-b", "0.0.0.0:8080", "healthcheck:app", "--timeout", "300", "--log-level=debug"]