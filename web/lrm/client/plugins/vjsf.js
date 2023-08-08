// Ref.: https://koumoul-dev.github.io/vuetify-jsonschema-form/latest/getting-started
import Vue from 'vue'
import VJsf from '@koumoul/vjsf'
import '@koumoul/vjsf/dist/main.css'
// load third-party dependencies (markdown-it, vuedraggable)
// you can also load them separately based on your needs
// import '@koumoul/vjsf/dist/third-party.js'
// -> failing => moved as "à la carte" import in nuxt.config.js
// Ref.: https://github.com/koumoul-dev/vuetify-jsonschema-form/issues/89
import Draggable from 'vuedraggable'
const _global = (typeof window !== 'undefined' && window) || (typeof global !== 'undefined' && global) || {}
_global.markdownit = require('markdown-it')

Vue.component('VJsf', VJsf)
// eslint-disable-next-line vue/multi-word-component-names
Vue.component('Draggable', Draggable)
