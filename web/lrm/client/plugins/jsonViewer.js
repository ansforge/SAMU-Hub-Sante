import JsonViewer from 'vue-json-viewer'

export default defineNuxtPlugin((app) => {
  app.vueApp.use(JsonViewer)
})
