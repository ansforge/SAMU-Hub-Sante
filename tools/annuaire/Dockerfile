# Use an official Python runtime as a parent image
FROM python:3.13-slim

# Set environment variables
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    UV_CACHE_DIR=/opt/uv-cache \
    UV_LINK_MODE=copy \
    ENVIRONMENT=production

# Install system dependencies and uv
RUN apt-get update && apt-get install -y \
    curl \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && curl -LsSf https://astral.sh/uv/install.sh | sh \
    && mv /root/.local/bin/uv /usr/local/bin/uv \
    && mv /root/.local/bin/uvx /usr/local/bin/uvx

# Create a non-root user and group
RUN groupadd -r -g 1000 annuaire && useradd -r -u 1000 -g annuaire annuaire

# Set the working directory in the container
WORKDIR /app

# Create UV cache directory with proper permissions and set ownership of working directory
RUN mkdir -p $UV_CACHE_DIR && \
    chown -R annuaire:annuaire $UV_CACHE_DIR && \
    chown -R annuaire:annuaire /app

# Switch to the non-root user
USER annuaire

# Copy the current directory contents into the container at /app
COPY --chown=annuaire:annuaire . .

# Install dependencies and project in one step
RUN uv sync --frozen --no-dev

# Make port 8080 available to the world outside this container
EXPOSE 8080

# Use Gunicorn to serve the Flask app (--no-sync prevents any dependency downloads at runtime)
CMD ["uv", "run", "--no-sync", "gunicorn", "-w", "4", "-b", "0.0.0.0:8080", "annuaire:app", "--timeout", "180", "--log-level=debug"]
