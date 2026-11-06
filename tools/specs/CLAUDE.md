# Site des specs

`client/` : React/Vite/TypeScript (pnpm). `server/` : Python (uv).

## Commandes
- Client : `pnpm install --frozen-lockfile`, `pnpm exec tsc --noEmit`, `pnpm build`.
- Serveur : `uv sync`, `uv run ruff format --check .`, `uv run ruff check .`, `uv run mypy .`, `uv run pytest -q`.

## Pièges
- pnpm pour le client, uv pour le serveur : ne pas mélanger avec npm.
- Une release avec le préfixe `specs-*` déclenche le build de l'image.
