// middleware/auth.js
import { defineNuxtRouteMiddleware, navigateTo } from 'nuxt/app';
import { useAuthStore } from '@/store/auth'; // Adjust the path as necessary

export default defineNuxtRouteMiddleware((to, _from) => {
  const authStore = useAuthStore();

  if (['/', '/json'].includes(to.path)) {
    return; // Allow access to these pages
  }

  if (!authStore.isAuthenticated) {
    return navigateTo('/'); // Redirect to login page if not authenticated
  }
});
