import { navigateTo } from 'nuxt/app';
import { defineStore } from 'pinia';
type User = {
  clientId: string;
  targetId: string;
  tester: boolean;
  advanced: boolean;
  showSentMessages: boolean;
  autoAck: boolean;
};

const initialUser: User = {
  clientId: '',
  targetId: '',
  tester: false,
  advanced: false,
  showSentMessages: false,
  autoAck: false,
};

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: initialUser,
  }),

  getters: {
    isAuthenticated: (state) => !!state?.user?.clientId,
    getUser: (state) => state.user,
  },

  actions: {
    login(userData: User) {
      this.setUser(userData);
    },

    setUser(userData: User) {
      this.user = userData;
    },

    logout() {
      this.user = initialUser;
      navigateTo('/');
    },
  },
});
