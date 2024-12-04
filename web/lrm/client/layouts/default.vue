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
      <v-btn data-cy="json-creator-button" class="ml-4" to="/json">
        <v-icon start>
          mdi-email-edit-outline
        </v-icon>
        Json Creator
      </v-btn>
      <v-spacer />
      <div class="mx-5" style="height: 20px; width: 20px;" @click="toggleAdvanced" />
      <span v-if="store.isAuthenticated" class="mr-2" style="cursor: pointer" @click="clickHandler">
        <v-icon color="rgb(100,100,100)">
          {{ userInfos.icon }}
        </v-icon>
        <b>{{ userInfos.name }}</b>
        <v-icon v-if="!store.isAdvanced" color="primary">
          mdi-arrow-right-thin
        </v-icon>
        <v-icon v-else color="primary">
          mdi-swap-horizontal
        </v-icon>
        <v-icon color="rgb(100,100,100)">
          {{ clientInfos(store.user.targetId).icon }}
        </v-icon>
        {{ clientInfos(store.user.targetId).name }}
      </span>
    </v-app-bar>
    <v-main style="padding-bottom: 0;">
      <v-container fluid class="h-100">
        <slot />
      </v-container>
    </v-main>
    <v-footer app >
      <span><a :href=" repositoryUrl + 'tree/' + $store.selectedVhost.modelVersion ">SAMU Hub Modeles - v{{ $store.selectedVhost.modelVersion }}</a> &copy; {{ new Date().getFullYear() }}</span>
    </v-footer>
  </v-app>
</template>

<script>
import { useMainStore } from '~/store'
import mixinUser from '~/mixins/mixinUser'
import { REPOSITORY_URL } from '~/constants'

export default {
  name: 'DefaultLayout',
  mixins: [mixinUser],
  data () {
    return {
      store: useMainStore(),
      repositoryUrl: REPOSITORY_URL.replace('raw.githubusercontent', 'github')
    }
  },
  computed: {
  },
  methods: {
    toggleAdvanced () {
      this.store.toggleAdvanced()
    },
    clickHandler () {
      if (this.store.isAdvanced) {
        // No control as this will anyway fail, user is expected to be advanced
        this.store.logInUser({
          ...this.store.user,
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

<style>
header.v-toolbar {
  position: sticky !important;
}
html {
  overflow-y: auto;
}
.v-main {
  background-color: rgba(0, 0, 0, 0.01);
  padding-bottom: 1.8rem !important;
  padding-top: 0 !important;
}

.Toastify__toast-body>div:last-child>div {
  line-break: anywhere;
}
</style>
