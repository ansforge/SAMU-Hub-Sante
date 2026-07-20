import {
  createRootRoute,
  createRoute,
  createRouter,
  Link,
  Outlet,
} from "@tanstack/react-router";
import { Footer } from "@/components/Footer";
import { MessageDetail } from "@/components/MessageDetail";
import { MessageList } from "@/components/MessageList";
import { useSchemaStore } from "@/store/schema-store";

function Root() {
  return (
    <div className="flex min-h-screen flex-col">
      <header className="px-6 py-4">
        <Link to="/">
          <img
            src={`${import.meta.env.BASE_URL}logo-ANS.svg`}
            alt="ANS"
            className="h-10"
          />
        </Link>
      </header>
      <main className="flex flex-1 flex-col">
        <Outlet />
      </main>
      <Footer />
    </div>
  );
}

function Home() {
  return (
    <div className="flex flex-1 flex-col">
      <MessageDetail />
      <MessageList />
    </div>
  );
}

// Example route showing the pattern: createRoute + getParentRoute, then
// register it in rootRoute.addChildren([...]) below.
function Test() {
  return (
    <div className="flex flex-1 flex-col items-center justify-center gap-4">
      <p>Test route — /test</p>
      <Link to="/" className="text-sm underline">
        back home
      </Link>
    </div>
  );
}

const rootRoute = createRootRoute({
  loader: async () => {
    const res = await fetch(
      `${import.meta.env.VITE_SPECS_API_DOMAIN}/schemas`,
    );
    if (!res.ok) throw new Error("not found");
    const data = await res.json();
    console.log(data);

    useSchemaStore.getState().setSchemasFromArray(data);
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

const testRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/test",
  component: Test,
});

const routeTree = rootRoute.addChildren([indexRoute, testRoute]);

export const router = createRouter({
  routeTree,
  basepath: import.meta.env.PROD ? "/specs" : "/",
});

declare module "@tanstack/react-router" {
  interface Register {
    router: typeof router;
  }
}
