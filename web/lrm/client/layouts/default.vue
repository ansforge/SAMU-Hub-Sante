<template>
  <v-app>
    <v-app-bar
      fixed
      app
    >
      <v-toolbar-title>Dashboard</v-toolbar-title>
      <v-btn class="ml-4" href="/openapi">
        <v-icon left>mdi-file-document-multiple-outline</v-icon>
        API docs
      </v-btn>
      <v-btn class="ml-4" href="/api-docs">
        <v-icon left>mdi-television-guide</v-icon>
        API UI
      </v-btn>
      <v-btn class="ml-4" href="/poll" target="_blank">
        <v-icon left>mdi-checkbox-marked-circle-outline</v-icon>
        Check
      </v-btn>
    </v-app-bar>
    <v-main>
      <v-container fluid>
        <Nuxt/>
      </v-container>
    </v-main>
    <v-footer
      app
    >
      <span>&copy; {{ new Date().getFullYear() }}</span>
    </v-footer>
  </v-app>
</template>

<script>
export default {
  name: 'DefaultLayout',
  data() {
    return {}
  }
}
</script>
