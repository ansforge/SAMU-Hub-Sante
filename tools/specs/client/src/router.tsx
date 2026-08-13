import type { ReactNode } from "react";
import {
  createRootRoute,
  createRoute,
  createRouter,
  Link,
  notFound,
  Outlet,
} from "@tanstack/react-router";
import { SchemaDetail, SchemaDetailSkeleton } from "@/components/schema-detail";
import { messageListUrl, defaultRef, preserveRefSearch } from "@/config";
import { useSchemaStore } from "@/store/schema-store";
import {
  SidebarInset,
  SidebarProvider,
  SidebarTrigger,
} from "./components/ui/sidebar";
import { AppSidebar } from "./components/app-sidebar";
import { Separator } from "@base-ui/react";
import { SchemaReference } from "./types";
import { buildGithubSchemaUrl } from "./lib/utils";
import { ensureSchemaLoaded } from "./lib/ensure-schema-loaded";
import RefSelector from "./components/ref-selector";

function Root({ children = <Outlet /> }: { children?: ReactNode }) {
  return (
    <SidebarProvider>
      <AppSidebar />
      <SidebarInset>
        <header className="flex h-16 shrink-0 items-center gap-2 transition-[width,height] ease-linear group-has-data-[collapsible=icon]/sidebar-wrapper:h-12">
          <div className="flex items-center gap-2 px-4">
            <SidebarTrigger className="-ml-1" />
            <Separator
              orientation="vertical"
              className="mr-2 data-[orientation=vertical]:h-4"
            />
            <RefSelector />
          </div>
        </header>
        <main className="flex flex-1 flex-col">{children}</main>
      </SidebarInset>
    </SidebarProvider>
  );
}

function SchemaPagePending() {
  return (
    <div className="flex min-h-0 flex-1 flex-col">
      <SchemaDetailSkeleton />
    </div>
  );
}

function SchemaNotFound() {
  const { schemaName } = schemaRoute.useParams();
  const { ref } = rootRoute.useSearch();

  return (
    <div className="flex min-h-0 flex-1 flex-col">
      <div className="flex flex-1 flex-col items-center justify-center gap-2 p-6 text-center">
        <p className="text-5xl font-semibold text-muted-foreground">404</p>
        <p className="text-lg font-medium">Schéma introuvable</p>
        <p className="text-sm text-muted-foreground">
          « {schemaName} » n'existe pas sur la branche{" "}
          <span className="font-mono">{ref}</span>.
        </p>
        <div className="mt-2 flex gap-4 text-sm">
          <Link
            to="/"
            search={preserveRefSearch}
            className="underline"
          >
            Retour à l'accueil
          </Link>
          {ref !== defaultRef && (
            <Link
              to="/$schemaName"
              params={{ schemaName }}
              search={{ ref: defaultRef }}
              className="underline"
            >
              Essayer sur {defaultRef}
            </Link>
          )}
        </div>
      </div>
    </div>
  );
}

function SchemaLoadError({ error, reset }: { error: unknown; reset: () => void }) {
  const message = error instanceof Error ? error.message : "Erreur inconnue";

  return (
    <div className="flex min-h-0 flex-1 flex-col">
      <div className="flex flex-1 flex-col items-center justify-center gap-2 p-6 text-center">
        <p className="text-lg font-medium">Impossible de charger le schéma</p>
        <p className="text-sm text-muted-foreground">{message}</p>
        <button type="button" onClick={() => reset()} className="mt-2 text-sm underline">
          Réessayer
        </button>
      </div>
    </div>
  );
}

function RootError({ error, reset }: { error: unknown; reset: () => void }) {
  const message = error instanceof Error ? error.message : "Erreur inconnue";

  return (
    <Root>
      <div className="flex flex-1 flex-col items-center justify-center gap-2 p-6 text-center">
        <p className="text-lg font-medium">Impossible de charger la liste des schémas</p>
        <p className="text-sm text-muted-foreground">{message}</p>
        <div className="mt-2 flex gap-4 text-sm">
          <button type="button" onClick={() => reset()} className="underline">
            Réessayer
          </button>
          <Link
            to="."
            search={{ ref: defaultRef }}
            onClick={() => reset()}
            className="underline"
          >
            Revenir sur {defaultRef}
          </Link>
        </div>
      </div>
    </Root>
  );
}

function Home() {
  return (
    <div className="flex flex-1 flex-col">
      <SchemaDetail />
    </div>
  );
}

function SchemaPage() {
  const schema = schemaRoute.useLoaderData();
  return (
    <div className="flex min-h-0 flex-1 flex-col">
      <SchemaDetail schema={schema} />
    </div>
  );
}

type RootSearch = { ref: string };

const schemaRoute = createRoute({
  getParentRoute: () => rootRoute,
  component: SchemaPage,
  path: "/$schemaName",
  loaderDeps: ({ search }: { search: RootSearch }) => ({ ref: search.ref }),
  loader: async ({ params, deps }) => {
    const schema = await ensureSchemaLoaded(params.schemaName, deps.ref);
    if (!schema) throw notFound();
    const res = await fetch(schema.url);
    if (res.status === 404) throw notFound();
    if (!res.ok) throw new Error(`Échec du chargement (HTTP ${res.status})`);
    return res.json();
  },
  staleTime: 30_000,
  pendingComponent: SchemaPagePending,
  // default pendingMs (1000) eats most of a 1-2s fetch before the skeleton
  // even shows up; show it right away instead
  pendingMs: 0,
  pendingMinMs: 300,
  notFoundComponent: SchemaNotFound,
  errorComponent: SchemaLoadError,
});

const rootRoute = createRootRoute({
  validateSearch: (search: Partial<RootSearch>): RootSearch => ({
    ref: typeof search.ref === "string" ? search.ref : defaultRef,
  }),
  loaderDeps: ({ search }) => ({ ref: search.ref }),
  loader: async ({ deps }) => {
    const res = await fetch(messageListUrl(deps.ref));
    if (res.status === 404) {
      throw new Error(`Branche ou tag "${deps.ref}" introuvable.`);
    }
    if (!res.ok) throw new Error(`Échec du chargement (HTTP ${res.status})`);
    const data = (await res.json()) as SchemaReference[];
    const schemas = data.map(({ label, schemaName }) => ({
      label,
      schemaName,
      url: buildGithubSchemaUrl(schemaName, deps.ref),
    }));
    useSchemaStore.getState().setSchemasFromArray(schemas, deps.ref);
    return data;
  },
  staleTime: 30_000,
  component: Root,
  pendingComponent: Root,
  pendingMs: 0,
  pendingMinMs: 0,
  errorComponent: RootError,
});
const indexRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/",
  component: Home,
});

const routeTree = rootRoute.addChildren([indexRoute, schemaRoute]);

export const router = createRouter({
  routeTree,
  basepath: import.meta.env.PROD ? "/specs" : "/",
});

declare module "@tanstack/react-router" {
  interface Register {
    router: typeof router;
  }
}
