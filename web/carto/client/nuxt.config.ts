import vuetify, { transformAssetUrls } from 'vite-plugin-vuetify';
import { defineNuxtConfig } from 'nuxt/config';

function isEnvProd() {
  return process.env.NODE_ENV === 'production';
}

export default defineNuxtConfig({
  ssr: true,
  vite: {
    plugins: [vuetify({ autoImport: true })],
    vue: {
      template: {
        transformAssetUrls,
      },
    },
  },

  // Global CSS: https://go.nuxtjs.dev/config-css
  css: ['vuetify/styles'],

  // Plugins to run before rendering page: https://go.nuxtjs.dev/config-plugins
  plugins: [],

  // Modules: https://go.nuxtjs.dev/config-modules
  modules: ['@pinia/nuxt'],

  // Build Configuration: https://go.nuxtjs.dev/config-build
  build: {
    // Necessary for "à la carte" import of vuetify components as the js import in vjsf.js was failing
    // Ref.: https://koumoul-dev.github.io/vuetify-jsonschema-form/latest/getting-started
    transpile: ['vuetify/lib'],
  },

  generate: {
    // Ignore href links of default.vue | Ref.: https://github.com/nuxt/nuxt.js/issues/8105#issuecomment-706702793
    exclude: [],
  },

  app: {
    baseURL: process.env.NODE_ENV === 'production' ? '/carto/' : '/',
    head: {
      link: [
        {
          rel: 'icon',
          type: 'image/x-icon',
          href: `${isEnvProd() ? '/carto/' : '/'}favicon.ico`,
        },
      ],
    },
  },

  runtimeConfig: {
    public: {
      backendCartoServer: 'localhost:8081',
      daeResourcesUrl:
        'https://tabular-api.data.gouv.fr/api/resources/edb6a9e1-2f16-4bbf-99e7-c3eb6b90794c/data/?c_etat_fonct__exact=En fonctionnement&page_size=50',
    },
  },

  compatibilityDate: '2025-03-03',

  // Configuration des assets
  dir: {
    assets: 'assets',
  },
});
