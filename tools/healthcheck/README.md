# Healthcheck app

## Setup

1. Create and activate a virtual environment (recommended):

```bash
uv venv
source .venv/bin/activate  # On Windows: .venv\Scripts\activate
```

2. Install dependencies:

```bash
uv sync
```

3. Create your local `.env` from the committed template, then adjust values if needed:

```bash
cp .env.example .env
```

The `.env` file is gitignored and only consumed by local commands (via `uv run --env-file .env`). In production the container receives env vars from the orchestration layer, so `.env` is also excluded from the Docker image via `.dockerignore`.

## Run healthcheck locally

```bash
uv run --env-file .env healthcheck.py --port 8085
```

## Tests

Run the tests:

```bash
uv run --env-file .env.test -m unittest tests/*.py -v
```

Tests use placeholder URLs that only need to satisfy `config.py` validation — they are intentionally kept inline rather than in `.env` so they cannot be confused with real local infra values.

Run the tests with coverage:

```bash
uv run --env-file .env.test coverage run -m unittest tests/*.py
```

Display the coverage report summary in the terminal:

```bash
uv run coverage report -m
```

Generate an HTML coverage report (open [htmlcov/index.html](htmlcov/index.html) afterwards):

```bash
uv run coverage html
```

Coverage doc available [here](https://coverage.readthedocs.io/en/7.8.0/).

## Lint and format

```bash
uv run ruff check --fix
uv run ruff format
```
