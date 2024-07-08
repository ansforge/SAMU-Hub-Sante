// import Vue from 'vue'
import JsonViewer from 'vue-json-viewer'

export default defineNuxtPlugin((app) => {
  app.vueApp.use(JsonViewer)
})
