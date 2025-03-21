import { defineNuxtPlugin } from 'nuxt/app';
import { useMainStore } from '@/store';
import type { Pinia } from 'pinia';

export default defineNuxtPlugin((nuxtApp) => {
  return {
    provide: {
      store: useMainStore(nuxtApp.$pinia as Pinia),
    },
  };
});