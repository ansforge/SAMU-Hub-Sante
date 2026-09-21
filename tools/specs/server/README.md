# Specs server

The documentation of the server for the specs services.

## Local run

run : 

```bash
uv run gunicorn -w 2 -b 0.0.0.0:8080 "specs:create_app()" --timeout 180 --log-level=debug
```

## Dev

Before committing, make sure you run `ruff`.

```bash
uv run ruff format .
uv run ruff check --fix
```