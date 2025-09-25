import vuetify, { transformAssetUrls } from 'vite-plugin-vuetify';
import { commonjsDeps } from '@koumoul/vjsf/utils/build.js';
import { defineNuxtConfig } from 'nuxt/config';
import istanbul from 'vite-plugin-istanbul';

function isEnvProd() {
  return process.env.NODE_ENV === 'production';
}

export default defineNuxtConfig({
  // Configuration Vue 3 + Nuxt 3
  vue: {
    runtimeCompiler: false, // ✅ Force la compilation à build-time
    compilerOptions: {
      isCustomElement: (tag) => tag.startsWith('custom-'),
    },
  },

  vite: {
    plugins: [
      vuetify({ autoImport: true }),
      istanbul({
        forceBuildInstrument: process.env.INSTRUMENT_BUILD === 'true',
        exclude: ['node_modules', 'cypress/', 'coverage/'],
        extension: ['.js', '.ts', '.vue'],
        cypress: true,
      }),
    ],
    vue: {
      template: {
        transformAssetUrls,
        compilerOptions: {
          isCustomElement: (tag) => tag.startsWith('ion-'),
        },
      },
    },
    build: {
      minify: 'terser',
      rollupOptions: {
        output: {
          // Éviter le code dynamique
          manualChunks: undefined,
        },
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
    transpile: ['vuetify/lib', '@koumoul/vjsf'],
  },

  // Configuration CSP native Nuxt 3
  ssr: true,
  nitro: {
    compressPublicAssets: true,
    // Configuration pour éviter eval dans le serveur
    experimental: {
      wasm: false,
    },
  },

  // Configuration existante...
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
            ['fr.health.test.samuA', ['fr.health.test.samuC']],
            ['fr.health.test.samuC', ['fr.health.test.samuA']],
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
