// import Vue from 'vue'
import JsonViewer from 'vue-json-viewer';
import { defineNuxtPlugin } from 'nuxt/app';

export default defineNuxtPlugin((app) => {
  app.vueApp.use(JsonViewer);
});
