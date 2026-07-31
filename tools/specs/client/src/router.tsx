import {
  createRootRoute,
  createRoute,
  createRouter,
  Outlet,
} from "@tanstack/react-router";
import { MessageDetailPlaceholder } from "@/components/message-detail";
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

function Home() {
  return (
    <div className="flex flex-1 flex-col">
      <MessageDetailPlaceholder />
    </div>
  );
}

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

const routeTree = rootRoute.addChildren([indexRoute]);

export const router = createRouter({
  routeTree,
  basepath: import.meta.env.PROD ? "/specs" : "/",
});

declare module "@tanstack/react-router" {
  interface Register {
    router: typeof router;
  }
}
