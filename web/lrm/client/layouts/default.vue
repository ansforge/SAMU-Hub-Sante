<template>
  <v-app>
    <v-app-bar>
      <v-toolbar-title
        style="cursor: pointer; flex: initial"
        @click="navigateTo('/')"
      >
        Hub Santé - LRM
      </v-toolbar-title>
      <v-btn class="ml-4" href="https://hub.esante.gouv.fr/" target="_blank">
        <v-icon start> mdi-presentation </v-icon>
        Page web
      </v-btn>
      <v-btn
        class="ml-4"
        href="https://hub.esante.gouv.fr/specs/"
        target="_blank"
      >
        <v-icon start> mdi-file-document-multiple-outline </v-icon>
        Specs
      </v-btn>
      <v-btn data-cy="json-creator-button" class="ml-4" to="/json">
        <v-icon start> mdi-email-edit-outline </v-icon>
        Json Creator
      </v-btn>
      <v-spacer />
      <div
        class="mx-5"
        style="height: 20px; width: 20px"
        @click="toggleAdvanced"
      />
      <span
        v-if="authStore.isAuthenticated"
        class="mr-2"
        style="cursor: pointer"
        @click="clickHandler"
      >
        <v-icon color="rgb(100,100,100)">
          {{ clientInfos().icon }}
        </v-icon>
        <b>{{ clientInfos().name }}</b>
        <v-icon v-if="!store.isAdvanced" color="primary">
          mdi-arrow-right-thin
        </v-icon>
        <v-icon v-else color="primary"> mdi-swap-horizontal </v-icon>
        <v-icon color="rgb(100,100,100)">
          {{ clientInfos(authStore.user.targetId).icon }}
        </v-icon>
        {{ clientInfos(authStore.user.targetId).name }}
      </span>
    </v-app-bar>
    <v-main style="padding-bottom: 0">
      <v-container fluid class="h-100">
        <slot />
      </v-container>
    </v-main>
    <v-footer>
      <span>
        <a :href="computedRepositoryUrl"
          >SAMU Hub Modeles - v{{ $store.selectedVhost.modelVersion }}</a
        >&nbsp;&copy; {{ new Date().getFullYear() }}</span
      >
      <!--Display websocket status if we're on /demo or /test or /test/*-->
      <span
        v-if="
          $route.path.startsWith('/demo') || $route.path.startsWith('/test')
        "
      >
        <span class="font-italic text-grey"
          >{{ readableWebsocketStatus }}
        </span>
        <v-icon
          class="ml-2"
          :color="store.isWebsocketConnected ? 'success' : 'warning'"
        >
          mdi-circle
        </v-icon>
      </span>
    </v-footer>
  </v-app>
</template>

<script>
import { useMainStore } from '~/store';
import { useAuthStore } from '@/store/auth'; // Adjust the path as necessary
import { REPOSITORY_URL } from '~/constants';
import { navigateTo } from 'nuxt/app';

export default {
  name: 'DefaultLayout',
  data() {
    return {
      store: useMainStore(),
      authStore: useAuthStore(),
      repositoryUrl: REPOSITORY_URL.replace('raw.githubusercontent', 'github'),
    };
  },
  computed: {
    readableWebsocketStatus() {
      return this.store.isWebsocketConnected
        ? 'Connexion établie'
        : 'Connexion en attente';
    },
    computedRepositoryUrl() {
      return (
        this.repositoryUrl + 'tree/' + this.$store.selectedVhost.modelVersion
      );
    },
  },
  methods: {
    toggleAdvanced() {
      this.store.toggleAdvanced();
    },
    clickHandler() {
      if (this.store.isAdvanced) {
        // No control as this will anyway fail, user is expected to be advanced
        this.store.logInUser({
          ...this.authStore.user,
          targetId: this.authStore.user.clientId,
          clientId: this.authStore.user.targetId,
        });
      } else {
        navigateTo('/');
      }
    },
  },
};
</script>

<style>
header.v-toolbar {
  position: sticky !important;
}

html {
  overflow-y: auto;
}

.v-main {
  background-color: rgb(0 0 0 / 1%);
  padding-bottom: 1.8rem !important;
  padding-top: 0 !important;
}

.Toastify__toast-body > div:last-child > div {
  line-break: anywhere;
}

.v-footer {
  z-index: 999;
  position: sticky;
  bottom: 0;
  display: flex;
  justify-content: space-between;
  max-height: fit-content;
}

.v-footer > span {
  align-content: center;
  display: flex;
}
</style>
