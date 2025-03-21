import { useRuntimeConfig } from 'nuxt/app';
import { defineStore } from 'pinia';
import { isEnvProd } from '~/composables/envUtils';

type User = {
  name: string;
  entity: {
    name: null;
    adress: null;
    lat: null;
    long: null;
    zoom: null;
  };
};

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null as User | null,
    config: useRuntimeConfig(),
  }),

  getters: {
    isAuthenticated: (state) => !!state?.user?.name,
    getUser: (state) => state.user,
  },

  actions: {
    async login(password: string) {
      const response = await fetch(this.getServerUrl() + '/login', {
        method: 'POST',
        body: JSON.stringify({ password: password }),
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/json',
        },
      });
      if (response.status != 200) {
        throw new Error('Bad response from server');
      }
      const result = await response.json();
      this.setUser(result.data);
    },

    setUser(userData: User) {
      this.user = userData;
    },

    logout() {
      this.user = null;
    },

    getServerUrl() {
      return `${isEnvProd() ? 'https' : 'http'}://${
        this.config.public.backendCartoServer
      }`;
    },
  },
});
