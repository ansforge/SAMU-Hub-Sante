# Annuaire API Flask

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

3. Create .env file
   ```bash
   cp .env.template .env
   ```

### Running the API

`annuaire.py` exposes a `create_app()` application factory (there is no module-level
`app` and no `python annuaire.py` entrypoint), so the app is started through Flask or
Gunicorn. The clients file is read from the path in `VALUES_PATH` (set in `.env`).

Development mode:
```bash
uv run --env-file .env flask --app annuaire run --port 8080
```

Production mode with Gunicorn:
```bash
ENVIRONMENT=production uv run --env-file .env gunicorn -w 4 -b 0.0.0.0:8080 "annuaire:create_app()"
```

### API Endpoints

- `GET /annuaire/api` - Returns the directory data as JSON
- `GET /annuaire/health` - Health check endpoint

### Tests

Run tests:
```bash
uv run python -m unittest test_annuaire.py
```

Run tests with coverage:
```bash
uv run coverage run --source=annuaire -m unittest test_annuaire.py
```

Display coverage report:
```bash
uv run coverage report -m
```

Generate HTML coverage report:
```bash
uv run coverage html
```

Open [htmlcov/index.html](htmlcov/index.html) in a browser to view the report.

Coverage doc available [here](https://coverage.readthedocs.io/en/7.8.0/)

### Development

Install development dependencies:
```bash
uv sync --group dev
```

Lint and format

```bash
uv run ruff check --fix
uv run ruff format
```

### Docker

Build and run with Docker:
```bash
docker build -t samu-hub-annuaire .
docker run -d -p 8080:8080 -v /path/to/config:/config samu-hub-annuaire
```

The API expects a CSV file at `/config/rabbitmq.clients-configuration.csv` inside the container.
