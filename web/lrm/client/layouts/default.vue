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
      <v-btn class="ml-4" to="/json">
        <v-icon left>
          mdi-email-edit-outline
        </v-icon>
        Json Creator
      </v-btn>
      <v-spacer />
      <div class="mx-5" style="height: 20px; width: 20px;" @click="toggleAdvanced" />
      <span v-if="isAuthenticated" class="mr-2" style="cursor: pointer" @click="clickHandler">
        <v-icon>
          {{ userInfos.icon }}
        </v-icon>
        <b>{{ userInfos.name }}</b>
        <v-icon v-if="!isAdvanced" color="primary">
          mdi-arrow-right-thin
        </v-icon>
        <v-icon v-else color="primary">
          mdi-swap-horizontal
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
import { mapGetters } from 'vuex'

export default {
  name: 'DefaultLayout',
  data () {
    return {}
  },
  computed: {
    ...mapGetters(['isAuthenticated', 'isAdvanced'])
  },
  methods: {
    toggleAdvanced () {
      this.$store.dispatch('toggleAdvanced')
    },
    clickHandler () {
      if (this.isAdvanced) {
        // No control as this will anyway fail, user is expected to be advanced
        this.$store.dispatch('logInUser', {
          ...this.user,
          targetId: this.user.clientId,
          clientId: this.user.targetId
        })
      } else {
        this.$router.push('/')
      }
    }
  }
}
</script>
