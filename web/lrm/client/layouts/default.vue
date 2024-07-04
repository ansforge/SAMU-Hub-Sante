<template>
  <v-app>
    <v-app-bar>
      <v-toolbar-title style="cursor: pointer; flex: initial" @click="navigateTo('/')">
        Hub Santé - LRM
      </v-toolbar-title>
      <v-btn class="ml-4" href="https://hub.esante.gouv.fr/" target="_blank">
        <v-icon start>
          mdi-presentation
        </v-icon>
        Page web
      </v-btn>
      <v-btn class="ml-4" href="https://hub.esante.gouv.fr/specs/" target="_blank">
        <v-icon start>
          mdi-file-document-multiple-outline
        </v-icon>
        Specs
      </v-btn>
      <v-btn class="ml-4" to="/json">
        <v-icon start>
          mdi-email-edit-outline
        </v-icon>
        Json Creator
      </v-btn>
      <v-spacer />
      <div class="mx-5" style="height: 20px; width: 20px;" @click="toggleAdvanced" />
      <span v-if="store.isAuthenticated" class="mr-2" style="cursor: pointer" @click="clickHandler">
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
          {{ clientInfos(store.user.targetId).icon }}
        </v-icon>
        {{ clientInfos(store.user.targetId).name }}
      </span>
    </v-app-bar>
    <v-main>
      <v-container fluid>
        <slot />
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
import { useMainStore } from '~/store'
import mixinUser from '~/mixins/mixinUser'

export default {
  name: 'DefaultLayout',
  mixins: [mixinUser],
  data () {
    return {
      store: useMainStore()
    }
  },
  computed: {
    // ...mapGetters(['isAuthenticated', 'isAdvanced'])
  },
  methods: {
    toggleAdvanced () {
      this.store.toggleAdvanced()
    },
    clickHandler () {
      if (this.isAdvanced) {
        // No control as this will anyway fail, user is expected to be advanced
        this.store.logInUser({
          ...this.user,
          targetId: this.store.user.clientId,
          clientId: this.store.user.targetId
        })
      } else {
        return navigateTo('/')
      }
    }
  }
}
</script>
