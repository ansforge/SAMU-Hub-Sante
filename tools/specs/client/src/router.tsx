import {
  createRootRoute,
  createRoute,
  createRouter,
  Link,
  Outlet,
} from "@tanstack/react-router";
import { Footer } from "@/components/Footer";
import { SchemaCards } from "@/components/SchemaCards";
import { SchemaDetail, SchemaDetailSkeleton } from "@/components/SchemaDetail";
import { SchemaList } from "@/components/SchemaList";
import { apiDomain } from "@/config";
import { useSchemaStore } from "@/store/schema-store";
import { ensureSchemaLoaded } from "@/lib/ensure-schema-loaded";

function Root() {
  return (
    <div className="flex h-screen flex-col overflow-hidden">
      <header className="shrink-0 px-6 py-4 border-b">
        <Link to="/">
          <img
            src={`${import.meta.env.BASE_URL}logo-ANS.svg`}
            alt="ANS"
            className="h-10"
          />
        </Link>
      </header>
      <main className="flex min-h-0 flex-1 flex-col">
        <Outlet />
      </main>
      <Footer />
    </div>
  );
}

function Home() {
  return (
    <div className="min-h-0 flex-1 overflow-y-auto">
      <SchemaCards />
    </div>
  );
}

function SchemaPage() {
  const schema = schemaRoute.useLoaderData();
  return (
    <div className="flex min-h-0 flex-1 flex-col">
      <SchemaDetail schema={schema} />
      <SchemaList />
    </div>
  );
}

function SchemaPagePending() {
  return (
    <div className="flex min-h-0 flex-1 flex-col">
      <SchemaDetailSkeleton />
      <SchemaList />
    </div>
  );
}

function SchemaNotFound() {
  return (
    <div className="flex min-h-0 flex-1 flex-col">
      <div className="flex flex-1 flex-col items-center justify-center gap-2 p-6 text-center">
        <p className="text-5xl font-semibold text-muted-foreground">404</p>
        <p className="text-lg font-medium">Schéma introuvable</p>
        <p className="text-sm text-muted-foreground">
          Ce schéma n'existe pas ou n'est plus disponible.
        </p>
        <Link to="/" className="mt-2 text-sm underline">
          Retour à l'accueil
        </Link>
      </div>
      <SchemaList />
    </div>
  );
}

function LoadError() {
  return (
    <div className="flex flex-1 flex-col items-center justify-center gap-2 p-6 text-center">
      <p className="text-lg font-medium">Impossible de charger les schémas</p>
      <p className="text-sm text-muted-foreground">
        Le service est peut-être indisponible. Réessayez plus tard.
      </p>
    </div>
  );
}

const rootRoute = createRootRoute({
  loader: async () => {
    const res = await fetch(`${apiDomain}/schemas`);
    if (!res.ok) throw new Error("not found");
    const data = await res.json();
    useSchemaStore.getState().setSchemasFromArray(data);
    return data;
  },
  staleTime: 30_000,
  component: Root,
  errorComponent: LoadError,
});
const indexRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/",
  component: Home,
});

const schemaRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/$schemaName",
  loader: async ({ params }) => {
    const schema = await ensureSchemaLoaded(params.schemaName);
    if (!schema) {
      throw new Error("Schema not found");
    }
    const res = await fetch(schema.url);
    if (!res.ok) throw new Error("not found");
    return res.json();
  },
  staleTime: 30_000,
  component: SchemaPage,
  pendingComponent: SchemaPagePending,
  // default pendingMs (1000) eats most of a 1-2s fetch before the skeleton
  // even shows up; show it right away instead
  pendingMs: 0,
  pendingMinMs: 300,
  errorComponent: SchemaNotFound,
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
