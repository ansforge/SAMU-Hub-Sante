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

## Run healthcheck locally

you can run:

```
make run-local
#> RABBITMQ_HEALTH_URL=http://localhost:15672/api/health/checks/alarms \
#> SHOVEL_STATUS_URL=http://shovel_test \
#> ANNUAIRE_HEALTH_URL=http://annuaire_test \
#> CONVERTER_HEALTH_URL=http://localhost:8083/health \
#> RABBITMQ_MONITORING_USERNAME=admin \
#> RABBITMQ_MONITORING_PASSWORD=admin \
#> DISPATCHER_CONFIG_FILE_PATH=dispatchers_config_file_path_example.txt \
#> uv run healthcheck.py --port 8085
```

## Tests

To run the tests: 

```
RABBITMQ_HEALTH_URL=http://test \
SHOVEL_STATUS_URL=http://shovel_test  \
ANNUAIRE_HEALTH_URL=http://annuaire_test  \
CONVERTER_HEALTH_URL=http://converter_test  \
RABBITMQ_MONITORING_USERNAME=test  \
RABBITMQ_MONITORING_PASSWORD=test  \
DISPATCHER_CONFIG_FILE_PATH=dispatchers_config_file_path_example.txt  \
uv run python -m unittest tests/*.py -v
```

To run the tests and generate a coverage report: `make test`

To display the coverage report summary in the terminal: `make show-coverage`

To generate a html report from an existing report: `uv run coverage html` & open [htmlcov/index.html](htmlcov/index.html) in a browser.

Coverage doc available [here](https://coverage.readthedocs.io/en/7.8.0/)
