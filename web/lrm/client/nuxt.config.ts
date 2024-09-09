import vuetify, { transformAssetUrls } from 'vite-plugin-vuetify'
import { commonjsDeps } from '@koumoul/vjsf/utils/build.js'
import { defineNuxtConfig } from 'nuxt/config'

export default defineNuxtConfig({
  ssr: true,
  vite: {
    vue: {
      template: {
        transformAssetUrls
      }
    },
    optimizeDeps: {
      include: commonjsDeps
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
    { src: '~/plugins/jsonViewer', mode: 'client' }
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
    transpile: ['vuetify/lib', '@koumoul/vjsf']
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
      clientMap: {},
      modelBranch: 'main',
      backendLrmServer: 'localhost:8081'
    }
  },

  compatibilityDate: '2024-09-04'
})
