import { defineNuxtConfig } from '@nuxt/bridge'

export default defineNuxtConfig({
  bridge: {
    typescript: true,
    nitro: true,
    capi: true
  },
  // Target: https://go.nuxtjs.dev/config-target
  target: 'server',

  meta: {
    titleTemplate: '%s - Hub Santé',
    title: 'LRM',
    htmlAttrs: {
      lang: 'fr'
    },
    meta: [
      { charset: 'utf-8' },
      { name: 'viewport', content: 'width=device-width, initial-scale=1' },
      { hid: 'description', name: 'description', content: '' },
      { name: 'format-detection', content: 'telephone=no' }
    ],
    link: [
      // Ref.: https://github.com/nuxt/nuxt/issues/10498#issuecomment-1160661667
      { rel: 'icon', type: 'image/x-icon', href: 'favicon.ico' }
    ]
  },

  // Global CSS: https://go.nuxtjs.dev/config-css
  css: [],

  // Plugins to run before rendering page: https://go.nuxtjs.dev/config-plugins
  plugins: [
    // Ref.: https://github.com/clwillingham/nuxt-vjsf-test/blob/master/nuxt.config.js
    { src: '~/plugins/vjsf', mode: 'client' },
    { src: '~/plugins/mixinUser', mode: 'client' },
    { src: '~/plugins/jsonViewer', mode: 'client' }
  ],

  // Modules: https://go.nuxtjs.dev/config-modules
  modules: [
    '@pinia/nuxt'
  ],

  // Build Configuration: https://go.nuxtjs.dev/config-build
  build: {
    // Necessary for "à la carte" import of vuetify components as the js import in vjsf.js was failing
    // Ref.: https://koumoul-dev.github.io/vuetify-jsonschema-form/latest/getting-started
    transpile: ['vuetify/lib', /@koumoul/, 'markdown-it/lib', 'vuedraggable/src']
  },

  generate: {
    // Ignore href links of default.vue | Ref.: https://github.com/nuxt/nuxt.js/issues/8105#issuecomment-706702793
    exclude: []
  },

  router: {
    middleware: ['auth'],
    base: (process.env.NODE_ENV === 'production' ? '/lrm/' : '/')
  },

  runtimeConfig: {
    public: {
      clientMap: process.env.CLIENT_MAP || '{}',
      modelBranch: process.env.MODEL_BRANCH || 'main',
      backendLrmServer: (process.env.BACKEND_LRM_SERVER === 'localhost'
        ? 'ws://localhost:8081/'
        : 'wss://' + process.env.BACKEND_LRM_SERVER + '/lrm/api/')
    }
  }
})
