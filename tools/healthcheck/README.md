# Healthcheck app

## Setup

1. Create and activate a virtual environment (recommended):

```bash
uv venv
source .venv/bin/activate  # On Windows: .venv\Scripts\activate
```

2. Install the package in development mode:

```bash
uv sync
```

## Tests

To run the tests: `RABBITMQ_URL=http://test RABBITMQ_MONITORING_USERNAME=test RABBITMQ_MONITORING_PASSWORD=test DISPATCHER_INSTANCES=dispatcher_instance uv run python -m unittest tests/*.py -v`

To run the tests and generate a coverage report: `make test`

To display the coverage report summary in the terminal: `make show-coverage`

To generate a html report from an existing report: `uv run coverage html` & open [htmlcov/index.html](htmlcov/index.html) in a browser.

Coverage doc available [here](https://coverage.readthedocs.io/en/7.8.0/)
