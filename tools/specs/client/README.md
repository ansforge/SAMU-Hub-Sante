# specs-client

Viewer for the Hub Santé message schemas: fetches the schema list from the specs API and displays it.

## Stack

- Vite
- React + TypeScript
- Tailwind CSS v4
- TanStack Router
- Zustand

## Getting started

```bash
cp .env.example .env # set VITE_SPECS_API_DOMAIN
pnpm install
pnpm dev
```

- `pnpm dev` — start the dev server
- `pnpm build` — build for production
- `pnpm preview` — preview the production build

## Structure

- `src/main.tsx` — app entry point
- `src/router.tsx` — router; root loader fetches `${VITE_SPECS_API_DOMAIN}/api/schemas` into the schema store, shared by all routes
- `src/store/schema-store.ts` — Zustand store holding schemas (by name) and the selected schema
- `src/components/MessageList.tsx` — bottom tab bar listing schemas, selects one
- `src/components/MessageDetail.tsx` — middle panel, schema detail (placeholder for now)
- `src/components/ui/button.tsx` — shadcn-style Button component
