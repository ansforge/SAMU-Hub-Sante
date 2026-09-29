import {
  createRootRoute,
  createRoute,
  createRouter,
  Link,
  notFound,
  Outlet,
} from "@tanstack/react-router";
import { SchemaDetail, SchemaDetailSkeleton } from "@/components/schema-detail";
import { defaultRef, preserveRefSearch, rawGithubDomain } from "@/config";
import { useSchemas } from "@/hooks/use-schemas";
import { SidebarInset, SidebarProvider } from "./components/ui/sidebar";
import { AppSidebar } from "./components/app-sidebar";
import { AppHeader } from "./components/app-header";
import { JsonSchemaDocument } from "./types";
import { buildGithubSchemaUrl } from "./lib/utils";

function Root() {
  const { error, refetch } = useSchemas();
  return (
    <SidebarProvider className="flex-col [--header-height:4.5rem]">
      <AppHeader />
      <div className="flex flex-1">
        <AppSidebar className="top-(--header-height) h-[calc(100svh-var(--header-height))]!" />
        <SidebarInset>
          <main className="flex flex-1 flex-col">
            {error ? (
              <SchemasError error={error} retry={refetch} />
            ) : (
              <Outlet />
            )}
          </main>
        </SidebarInset>
      </div>
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
          <Link to="/" search={preserveRefSearch} className="underline">
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

function SchemaLoadError({
  error,
  reset,
}: {
  error: unknown;
  reset: () => void;
}) {
  const message = error instanceof Error ? error.message : "Erreur inconnue";

  return (
    <div className="flex min-h-0 flex-1 flex-col">
      <div className="flex flex-1 flex-col items-center justify-center gap-2 p-6 text-center">
        <p className="text-lg font-medium">Impossible de charger le schéma</p>
        <p className="text-sm text-muted-foreground">{message}</p>
        <button
          type="button"
          onClick={reset}
          className="mt-2 text-sm underline"
        >
          Réessayer
        </button>
      </div>
    </div>
  );
}

function SchemasError({ error, retry }: { error: Error; retry: () => void }) {
  return (
    <div className="flex flex-1 flex-col items-center justify-center gap-2 p-6 text-center">
      <p className="text-lg font-medium">
        Impossible de charger la liste des schémas
      </p>
      <p className="text-sm text-muted-foreground">{error.message}</p>
      <div className="mt-2 flex gap-4 text-sm">
        <button type="button" onClick={retry} className="underline">
          Réessayer
        </button>
        <Link to="." search={{ ref: defaultRef }} className="underline">
          Revenir sur {defaultRef}
        </Link>
      </div>
    </div>
  );
}

function Home() {
  return (
    <div className="flex flex-1 items-center justify-center text-muted-foreground">
      Sélectionnez un schéma pour voir ses détails.
    </div>
  );
}

function SchemaPage() {
  const schema = schemaRoute.useLoaderData();
  const { schemaName } = schemaRoute.useParams();
  const { data: schemas } = useSchemas();
  const examples = schemas?.find((s) => s.schemaName === schemaName)?.examples;
  const { ref } = rootRoute.useSearch();
  return (
    <div className="flex min-h-0 flex-1 flex-col">
      <SchemaDetail
        schema={schema}
        examples={examples}
        schemaName={schemaName}
        ref={ref}
      />
    </div>
  );
}

type RootSearch = { ref: string };

const schemaRoute = createRoute({
  getParentRoute: () => rootRoute,
  component: SchemaPage,
  path: "/$schemaName",
  loaderDeps: ({ search }) => ({ ref: search.ref }),
  loader: async ({ params, deps }) => {
    const res = await fetch(
      buildGithubSchemaUrl(rawGithubDomain, deps.ref, params.schemaName),
    );
    if (res.status === 404) throw notFound();
    if (!res.ok) throw new Error(`Échec du chargement (HTTP ${res.status})`);
    return res.json() as Promise<JsonSchemaDocument>;
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
  component: Root,
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
