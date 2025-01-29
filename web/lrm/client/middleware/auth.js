// middleware/auth.js
export default function ({ route, store, redirect }) {
  if (['/', '/json'].includes(route.path)) {
    return;
  }
  if (!store.getters.isAuthenticated) {
    return redirect('/'); // Redirect to login page if not authenticated
  }
}
