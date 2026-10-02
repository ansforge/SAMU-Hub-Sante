# specs-client

Viewer for the Hub Santé message schemas, read from the `ansforge/SAMU-Hub-Modeles` GitHub repo for a given branch or tag.

## Stack

- Vite
- React + TypeScript
- Tailwind CSS v4
- TanStack Router
- TanStack Query
- Zustand (UI state only)

## Getting started

```bash
cp .env.example .env
pnpm install
pnpm dev
```

- `pnpm dev` — start the dev server
- `pnpm build` — build for production
- `pnpm preview` — preview the production build

## Configuration

| Variable | Description |
| --- | --- |
| `VITE_SPECS_API_DOMAIN` | Specs API (auth, branches and tags) |
| `VITE_SPECS_PUBLIC_VERSIONS` | JSON array of the versions visible when logged out, e.g. `["2.4.0", "3.5.0-rc.1"]`; the newest is the default. Unset or empty: falls back to `main` |
| `VITE_SPECS_VHOST_MAP` | JSON object from hubsante-topology's `vhost.map`; every `model_lib_version` is merged (deduplicated) into the public versions; for a ref targeted by some vhost, the perimeter filter comes from `supported_messages` (perimeter = vhost name before `_`, messages matched on `label`) instead of `perimeters` in `messagesList.json` |

In the Docker image, both are read at container start by `docker-entrypoint.sh` and written to `env-config.js`.

## Structure

- `src/main.tsx` — app entry point; resolves auth, then mounts the router with it in its context
- `src/router.tsx` — routes; the root `beforeLoad` resolves the `ref` search param (default `main` when logged in, newest public version otherwise) and restricts logged-out users to the public versions
- `src/config.ts` — env config, public versions, GitHub URLs
- `src/hooks/use-schemas.ts` — schema list of the current ref (`messagesList.json`), keeps the previous list while switching refs
- `src/hooks/use-auth.ts`, `use-refs.ts`, `use-nomenclature.ts` — auth, ref selector options, nomenclatures
- `src/store/schema-store.ts` — Zustand store for UI state (nomenclature drawer, expand/collapse all)
- `src/components/` — layout (`app-header`, `app-sidebar`, `nav-schemas`), `ref-selector`, `global-search`, `schema-detail/`
