# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Is

A Flask-based health monitoring service for Hub Santé infrastructure. It checks the status of internal components (RabbitMQ, Converter, Dispatchers, Annuaire, Hubex Partners Shovels) and exposes results via HTTP endpoints and Prometheus metrics.

## Commands

```bash
# Run locally — loads env vars from .env (see README "Setup")
uv run --env-file .env healthcheck.py --port 8085

# Tests + coverage (env vars must be set inline — see README for full command)
uv run coverage run -m unittest tests/*.py
uv run coverage report -m

# Lint / format
uv run ruff check --fix
uv run ruff format
```

Package management uses `uv` (not pip). Dependencies are in `pyproject.toml` with `uv.lock`. There is no Makefile — all commands are invoked directly via `uv run`. Required env vars and the `.env` workflow are documented in `README.md`.

## Architecture

**Endpoints** (defined in `healthcheck.py`):
- `/health` — external: checks RabbitMQ, Dispatchers, Converter, Hubex Partners Shovels
- `/internal/health` — internal: all 5 components (adds Annuaire)
- `/metrics` — Prometheus scrape, triggers all internal checks before responding

**Check pattern**: Each check in `checks/` extends `IChecker` (from `checker.py`) and implements:
- `perform_checks()` — main check logic (HTTP call to component, returns status dict)
- `check_failure_fallback()` — fallback response when exception occurs

`check_wrapper()` in the base class handles exception catching and logging.

Each checker exposes a Prometheus Gauge metric (1=UP, 0=DOWN) and returns a status dict. Global status is UP only if all components are UP.

**Config** (`config.py`): Requires env vars `RABBITMQ_URL`, `RABBITMQ_MONITORING_USERNAME`, `RABBITMQ_MONITORING_PASSWORD`, `DISPATCHER_INSTANCES` (comma-separated). Optional `HTTP_TIMEOUT` (default 5s).

**Hubex Partners Shovels** is the most complex check — it reads `monitored_partners_shovels.txt` (format: `vhost;queue1,queue2`) and verifies each RabbitMQ shovel's `blocked_status` is "running".

## Testing

Tests use `unittest` with `parameterized` for data-driven tests. All external HTTP calls are mocked. Main test file is `tests/test_healthcheck.py`. Run a single test:

```bash
RABBITMQ_URL=http://test RABBITMQ_MONITORING_USERNAME=test RABBITMQ_MONITORING_PASSWORD=test DISPATCHER_INSTANCES=dispatcher_instance uv run python -m unittest tests.test_healthcheck.TestHealthcheck.test_method_name
```
