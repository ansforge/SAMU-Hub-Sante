# Outils

Outils annexes du Hub. `specs` a son propre `CLAUDE.md`.

## Python (`annuaire`, `healthcheck`)
- Gestionnaire : uv. Commandes : `uv sync`, `uv run ruff format --check .`, `uv run ruff check .`, `uv run pytest -q`.
- `annuaire` : API Flask de l'annuaire. `healthcheck` : monitoring.

## Tests de bout en bout et de charge
- `tnr` (Java/Gradle) : non-régression contre un vrai RabbitMQ, nécessite `.env` et certificats TLS. Cible qualification ou bac à sable, via le workflow manuel `run-tnr`.
- `tmc` (Gatling, Java/Gradle) : tests de charge.

## Pièges
- Ne jamais commiter `.env`, certificats ni clés.
