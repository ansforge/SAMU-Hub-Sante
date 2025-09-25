import { defineNuxtPlugin } from 'nuxt/app';

export default defineNuxtPlugin(async (app) => {
  // Only load on client-side to avoid SSR issues with window/document references
  if (process.client) {
    const { default: JsonViewer } = await import('vue-json-viewer');
    app.vueApp.use(JsonViewer);
  }
});
