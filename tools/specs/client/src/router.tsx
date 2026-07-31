import {
  createRootRoute,
  createRoute,
  createRouter,
  Link,
  Outlet,
} from "@tanstack/react-router";
import { SchemaDetail, SchemaDetailSkeleton } from "@/components/schema-detail";
import { messageListUrl, defaultBranch } from "@/config";
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

function Root() {
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
          </div>
        </header>
        <main className="flex flex-1 flex-col">
          <Outlet />
        </main>
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
    </div>
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

const schemaRoute = createRoute({
  getParentRoute: () => rootRoute,
  component: SchemaPage,
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
  pendingComponent: SchemaPagePending,
  // default pendingMs (1000) eats most of a 1-2s fetch before the skeleton
  // even shows up; show it right away instead
  pendingMs: 0,
  pendingMinMs: 300,
  errorComponent: SchemaNotFound,
});

const rootRoute = createRootRoute({
  loader: async () => {
    const res = await fetch(messageListUrl);
    if (!res.ok) throw new Error("not found");
    const data = (await res.json()) as SchemaReference[];
    const schemas = data.map(({ label, schemaName }) => ({
      label,
      schemaName,
      url: buildGithubSchemaUrl(schemaName, defaultBranch),
    }));
    useSchemaStore.getState().setSchemasFromArray(schemas);
    return data;
  },
  staleTime: 30_000,
  component: Root,
  errorComponent: (error) => (
    <div className="flex flex-col items-center justify-center gap-6">
      <pre>
        <code>{JSON.stringify(error, null, 2)}</code>
      </pre>
    </div>
  ),
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
