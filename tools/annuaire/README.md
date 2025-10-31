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

### Running the API

Development mode:
```bash
uv run python annuaire.py
```

Production mode with Gunicorn:
```bash
ENVIRONMENT=production uv run gunicorn -w 4 -b 0.0.0.0:8080 annuaire:app
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

### Development

Install development dependencies:
```bash
uv sync --group dev
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
docker build -t samu-hub-annuaire .
docker run -d -p 8080:8080 -v /path/to/config:/config samu-hub-annuaire
```

The API expects a CSV file at `/config/rabbitmq.clients-configuration.csv` inside the container.
