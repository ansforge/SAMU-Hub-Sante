<template>
  <v-app>
    <v-app-bar
      fixed
      app
    >
      <v-toolbar-title style="cursor: pointer" @click="$router.push('/')">
        Hub Santé - LRM
      </v-toolbar-title>
      <v-btn class="ml-4" href="https://hub.esante.gouv.fr/" target="_blank">
        <v-icon left>
          mdi-presentation
        </v-icon>
        Page web
      </v-btn>
      <v-btn class="ml-4" href="https://hub.esante.gouv.fr/specs/" target="_blank">
        <v-icon left>
          mdi-file-document-multiple-outline
        </v-icon>
        Specs
      </v-btn>
      <v-btn class="ml-4" href="https://hub.esante.gouv.fr/jsv/" target="_blank">
        <v-icon left>
          mdi-file-tree
        </v-icon>
        JSV
      </v-btn>
      <v-spacer />
      <span v-if="isAuthenticated" class="mr-2" style="cursor: pointer" @click="$router.push('/')">
        <v-icon>
          {{ userInfos.icon }}
        </v-icon>
        {{ userInfos.name }}
        <v-icon color="primary">
          mdi-arrow-right-thin
        </v-icon>
        <v-icon>
          {{ clientInfos(user.targetId).icon }}
        </v-icon>
        {{ clientInfos(user.targetId).name }}
      </span>
    </v-app-bar>
    <v-main>
      <v-container fluid>
        <Nuxt />
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
  data () {
    return {}
  }
}
</script>
