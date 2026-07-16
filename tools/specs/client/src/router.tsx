import {
  createRootRoute,
  createRoute,
  createRouter,
  Link,
  Outlet,
} from "@tanstack/react-router"
import { Button } from "@/components/ui/button"
import { Footer } from "@/components/Footer"
import { useCounterStore } from "@/store/counter-store"

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
  )
}

function Home() {
  const { count, increment, decrement } = useCounterStore()

  return (
    <div className="flex flex-1 flex-col items-center justify-center gap-6">
      <p className="text-4xl font-semibold">{count}</p>
      <div className="flex gap-3">
        <Button variant="default" onClick={increment}>
          Increment
        </Button>
        <Button variant="secondary" onClick={decrement}>
          Decrement
        </Button>
      </div>
      <Link to="/test" className="text-sm underline">
        go to /test
      </Link>
    </div>
  )
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
  )
}

const rootRoute = createRootRoute({ component: Root })

const indexRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/",
  component: Home,
})

const testRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/test",
  component: Test,
})

const routeTree = rootRoute.addChildren([indexRoute, testRoute])

export const router = createRouter({
  routeTree,
  basepath: import.meta.env.PROD ? "/specs" : "/",
})

declare module "@tanstack/react-router" {
  interface Register {
    router: typeof router
  }
}
