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

## Hubex partners connections

Two checks monitor the connections with the Hubex partners (e.g. Hub Pompiers):

| Direction | Check | Config file | How the status is determined |
| --- | --- | --- | --- |
| Hubex -> Hub Santé | `checks/hubex_partners_shovels.py` | `monitored_partners_shovels.txt` | The shovel (`SHOVEL_STATUS_URL`) that fetches the partner messages is `running` |
| Hub Santé -> Hubex | `checks/hubex_consumers.py` | `monitored_hubex_queues.txt` | The queues made available to the partner (`QUEUES_URL`) have at least one consumer |

Both config files use one line per vhost, with the monitored queues separated by commas:

```
15-nexsis_vactive;fr.health.fire.message,fr.health.fire.ack
```

For the consumers check, each queue is exposed in the response as `<vhost>-<queue>-consumers` and in the Prometheus metric `hubex_consumers_status`. A queue is `DOWN` when it has no consumer, when it does not exist or when the RabbitMQ API cannot be reached. The `info` queue is not monitored since the partners choose not to consume it.

Each connection is also summarized per vhost in the response: `<vhost>-shovels` (Hubex -> Hub Santé) and `<vhost>-consumers` (Hub Santé -> Hubex). The status is `UP` when all the shovels (or queues) of the vhost are `UP`, `DOWN` when all are `DOWN` and `DEGRADED` otherwise. The same aggregation applies to the dispatchers, in the `dispatchers` entry.

`QUEUES_URL` is the RabbitMQ management API queues endpoint, restricted to the needed columns: `http://localhost:15672/api/queues?columns=vhost,name,consumers`.

## Platform status

In addition to `components`, `/health` and `/internal/health` return a `perimeters` block and a `technical` block, and the global `status` is computed from them (`UP`, `DEGRADED` or `DOWN`, always with HTTP 200). The rules are in `checks/platform_status.py`.

Each line of the dispatchers config file has an optional perimeter: `<name>;<url>;<perimeter>`, with `15-15` or `15-18`. A warning is logged for a dispatcher without perimeter.

| Item | `DOWN` | `DEGRADED` |
| --- | --- | --- |
| `perimeters.15-15` (15-15 and 15-CAP) | all the 15-15 dispatchers are `DOWN` | a 15-15 dispatcher or the converter is `DOWN` |
| `perimeters.15-18.routing` | the 15-18 dispatcher is `DOWN`, or all the dispatchers are `DOWN` | a dispatcher is not `UP`, the converter is `DOWN` or MongoDB is unreachable |
| Global `status` | RabbitMQ is `DOWN` or all the perimeters are `DOWN` | a perimeter or a `technical` item is not `UP` |

A `DEGRADED` dispatcher (MongoDB unreachable) only affects the 15-18 routing. The annuaire does not impact the status.

`perimeters.15-18.fire_connections` exposes the connections with the Hubex fire: `fire_to_health` (shovels) and `health_to_fire` (consumers). They depend on the partner, so they change neither the routing nor the global status. The `fire_connections` key is omitted when there is nothing to monitor.

`technical` gives the status of `rabbitmq`, `dispatcher`, `converter` and `persistence` (MongoDB).

## MongoDB metrics

The connection to MongoDB is optional for the converter and the dispatchers: when it is lost they answer `DEGRADED` instead of `DOWN`. The healthcheck publishes it on `/metrics`:

| Metric | Source | Value |
| --- | --- | --- |
| `dispatcher_status{dispatcher}` | `status` of the dispatcher | `1` if `UP` or `DEGRADED`, `0` otherwise |
| `dispatcher_mongodb_status{dispatcher}` | `components.mongo.status` of the dispatcher | `1` if `UP`, `0` otherwise |
| `converter_status` | `status` of the converter | `1` if `UP` or `DEGRADED`, `0` otherwise |
| `converter_mongodb_status` | `components.mongodb` of the converter | `1` if `UP`, `0` otherwise |

`*_mongodb_status` is only published while the component answers (and returns the MongoDB component). When it is unreachable the series is absent, so that a single alert is raised for a given component:

| State | `*_status` | `*_mongodb_status` |
| --- | --- | --- |
| `UP` | `1` | `1` |
| `DEGRADED` (MongoDB unreachable) | `1` | `0` |
| Unreachable or `DOWN` | `0` | absent |

A `DEGRADED` converter is reported `DEGRADED` in `components.converter`, and no longer `DOWN`.

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
