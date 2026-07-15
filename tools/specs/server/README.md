# Specs server

The documentation of the server for the specs services.

## Local run

run : 

```bash
uv run gunicorn -w 2 -b 0.0.0.0:8086 "specs:create_app()" --timeout 180 --log-level=debug
```