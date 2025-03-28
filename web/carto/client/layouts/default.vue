<template>
  <v-app>
    <v-app-bar height="100" :flat="true">
      <v-img
        src="~/assets/images/logo_ministere.png"
        max-width="78"
        class="ma-4"
        alt="Logo ANS"
      />
      <v-img
        src="~/assets/images/logo_ANS.png"
        max-width="178"
        class="ma-4"
        alt="Logo ANS"
      />
      <v-spacer />
      <v-app-bar-title>Hub Santé Géoloc</v-app-bar-title>
      <v-spacer />
      <v-btn
        v-if="authStore.isAuthenticated"
        prepend-icon="mdi-account-circle"
        stacked
        variant="text"
        size="large"
        @click.prevent
      >
        {{ authStore.user.name }}
        <v-menu activator="parent">
          <v-list>
            <v-list-item style="cursor: pointer" @click="logout()">
              <v-list-item-title>Déconnexion</v-list-item-title>
            </v-list-item>
          </v-list>
        </v-menu>
      </v-btn>
    </v-app-bar>
    <v-main style="padding-bottom: 0">
      <v-container fluid class="h-100">
        <slot />
      </v-container>
    </v-main>
    <v-footer app>
      <!-- Display websocket status if we're on /carto -->
      <span v-if="$route.path.startsWith('/carto')">
        <span class="font-italic text-grey"
          >{{ readableWebsocketStatus }}
        </span>
        <v-icon
          class="ml-2"
          icon="mdi-circle"
          :color="store.isWebsocketConnected ? 'success' : 'warning'"
        />
      </span>
      <span>Hub Santé - Geoloc - v1.0</span>
    </v-footer>
  </v-app>
</template>

<script>
import { useMainStore } from '~/store';
import { useAuthStore } from '@/store/auth';
import { navigateTo } from 'nuxt/app';

export default {
  name: 'DefaultLayout',
  setup() {
    return {
      store: useMainStore(),
      authStore: useAuthStore(),
    };
  },
  computed: {
    readableWebsocketStatus() {
      return this.store.isWebsocketConnected
        ? 'Connexion établie'
        : 'Connexion en attente';
    },
  },
  methods: {
    async logout() {
      this.authStore.logout();
      navigateTo('/');
    },
  },
};
</script>

<style>
html {
  overflow-y: auto;
}

.v-main {
  background-color: rgba(0, 0, 0, 0.01);
  padding-bottom: 1.8rem !important;
  padding-top: 0 !important;
}

header.v-toolbar {
  position: sticky !important;
}

.v-app-bar-title {
  color: #343852;
  font-weight: 500 !important;
  font-size: 28px !important;
}

.Toastify__toast-body>div:last-child>div {
  line-break: anywhere;
}

.v-footer {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.v-footer>span {
  align-content: center;
  display: flex;
}

.v-img {
  height: 100%;
  display: flex;
  align-items: center;
}
</style>
