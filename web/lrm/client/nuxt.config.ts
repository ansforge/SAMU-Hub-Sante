import vuetify, { transformAssetUrls } from 'vite-plugin-vuetify';
import { commonjsDeps } from '@koumoul/vjsf/utils/build.js';
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
    optimizeDeps: {
      include: commonjsDeps,
    },
  },

  // Global CSS: https://go.nuxtjs.dev/config-css
  css: ['vuetify/styles'],

  // Plugins to run before rendering page: https://go.nuxtjs.dev/config-plugins
  plugins: [{ src: '~/plugins/jsonViewer', mode: 'client' }],

  // Modules: https://go.nuxtjs.dev/config-modules
  modules: ['@pinia/nuxt'],

  // Build Configuration: https://go.nuxtjs.dev/config-build
  build: {
    // Necessary for "à la carte" import of vuetify components as the js import in vjsf.js was failing
    // Ref.: https://koumoul-dev.github.io/vuetify-jsonschema-form/latest/getting-started
    transpile: ['vuetify/lib', '@koumoul/vjsf'],
  },

  generate: {
    // Ignore href links of default.vue | Ref.: https://github.com/nuxt/nuxt.js/issues/8105#issuecomment-706702793
    exclude: [],
  },

  app: {
    baseURL: isEnvProd() ? '/lrm/' : '/',
    head: {
      link: [
        {
          rel: 'icon',
          type: 'image/x-icon',
          href: `${isEnvProd() ? '/lrm/' : '/'}favicon.ico`,
        },
      ],
    },
  },

  runtimeConfig: {
    public: {
      clientMap: !isEnvProd()
        ? [
            ['fr.health.samuA', ['fr.health.samuC']],
            ['fr.health.samuC', ['fr.health.samuA']],
          ]
        : {},
      vhostMap: !isEnvProd()
        ? {
            '15-15_v1.5': '1.0.0',
            '15-nexsis_v1.8': '1.0.0',
            '15-smur_v1.4': '1.0.0',
            '15-gps_v1.0': '1.0.0',
          }
        : {},
      backendLrmServer: 'localhost:8081',
    },
  },

  compatibilityDate: '2024-09-04',
});
