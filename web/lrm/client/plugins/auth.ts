import { defineNuxtPlugin } from 'nuxt/app';
import { useAuthStore } from '~/store/auth';

export default defineNuxtPlugin(() => {
  const authStore = useAuthStore();
  authStore.initializeAuth(); // Load token from localStorage
});
