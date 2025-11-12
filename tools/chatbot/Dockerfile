# Use an official Python runtime as a parent image
FROM python:3.13-slim

# Set environment variables
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    UV_CACHE_DIR=/opt/uv-cache \
    UV_LINK_MODE=copy

# Install system dependencies and uv
RUN apt-get update && apt-get install -y \
    curl \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && curl -LsSf https://astral.sh/uv/install.sh | sh \
    && mv /root/.local/bin/uv /usr/local/bin/uv \
    && mv /root/.local/bin/uvx /usr/local/bin/uvx

# Create a non-root user and group
RUN groupadd -r -g 1000 chatbot && useradd -r -u 1000 -g chatbot chatbot

# Set the working directory in the container
WORKDIR /app

# Create UV cache directory with proper permissions and set ownership of working directory
RUN mkdir -p $UV_CACHE_DIR && \
    chown -R chatbot:chatbot $UV_CACHE_DIR && \
    chown -R chatbot:chatbot /app

# Switch to the non-root user
USER chatbot

# Copy the current directory contents into the container at /app
COPY --chown=chatbot:chatbot . .

# Install dependencies and project in one step
RUN uv sync --frozen --no-dev

# Run the application (--no-sync prevents any dependency downloads at runtime)
CMD ["uv", "run", "--no-sync", "python", "chatbot.py"]