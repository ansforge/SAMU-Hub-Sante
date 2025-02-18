// import this after install `@mdi/font` package
import { createVuetify } from 'vuetify';
import 'vuetify/styles';
import '@mdi/font/css/materialdesignicons.css';
import { defineNuxtPlugin } from 'nuxt/app';

export default defineNuxtPlugin((app) => {
  const vuetify = createVuetify({
    ssr: true,
  });
  app.vueApp.use(vuetify);
});
