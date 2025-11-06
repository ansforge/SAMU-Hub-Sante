# SAMU Hub Santé Chatbot

A Slack chatbot for answering questions about the SAMU Hub Santé platform using RAG (Retrieval Augmented Generation).

## Setup

This project uses [uv](https://docs.astral.sh/uv/) for dependency management.

### Prerequisites

- Python 3.11+
- uv package manager

### Installation

1. Install uv if you haven't already:
   ```bash
   curl -LsSf https://astral.sh/uv/install.sh | sh
   ```

2. Install dependencies:
   ```bash
   uv sync
   ```

### Running the Chatbot

```bash
uv run python chatbot.py
```

### Environment Variables

Create a `.env` file with the following variables:

```
SLACK_APP_TOKEN=xapp-...
SLACK_BOT_TOKEN=xoxb-...
OPENAI_API_KEY=sk-...
DSF_URL=path/to/dsf.pdf
DST_URL=path/to/dst.pdf
```

### Development

Install development dependencies:

```bash
uv sync --group dev
```

Run tests:

```bash
uv run pytest
```

Format code:

```bash
uv run black .
```

Lint code:

```bash
uv run flake8 .
```

### Docker

Build and run with Docker:

```bash
docker build -t samu-hub-chatbot .
docker run -d --env-file .env samu-hub-chatbot
```