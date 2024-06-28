import { defineNuxtConfig } from '@nuxt/bridge'
import vuetify, { transformAssetUrls } from 'vite-plugin-vuetify'

export default defineNuxtConfig({
  bridge: {
    typescript: true,
    nitro: true,
    capi: true,
    vite: true
  },
  vite: {
    vue: {
      template: {
        transformAssetUrls
      }
    }
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
  ],

  // Modules: https://go.nuxtjs.dev/config-modules
  modules: [
    '@pinia/nuxt',
    (_options, nuxt) => {
      nuxt.hooks.hook('vite:extendConfig', (config) => {
        // @ts-expect-error
        config.plugins.push(vuetify({ autoImport: true }))
      })
    }
  ],

  // Build Configuration: https://go.nuxtjs.dev/config-build
  build: {
    // Necessary for "à la carte" import of vuetify components as the js import in vjsf.js was failing
    // Ref.: https://koumoul-dev.github.io/vuetify-jsonschema-form/latest/getting-started
    transpile: ['vuetify', 'markdown-it/lib']
  },

  generate: {
    // Ignore href links of default.vue | Ref.: https://github.com/nuxt/nuxt.js/issues/8105#issuecomment-706702793
    exclude: []
  },

  app: {
    baseURL: (process.env.NODE_ENV === 'production' ? '/lrm/' : '/')
  },

  router: {
    middleware: ['auth']
  },

  runtimeConfig: {
    public: {
      clientMap: process.env.CLIENT_MAP || {},
      modelBranch: process.env.MODEL_BRANCH || 'main',
      backendLrmServer: (process.env.BACKEND_LRM_SERVER === 'localhost'
        ? 'ws://localhost:8081/'
        : 'wss://' + process.env.BACKEND_LRM_SERVER + '/lrm/api/')
    }
  }
})
