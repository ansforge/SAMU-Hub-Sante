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

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null as User | null,
  }),

  getters: {
    isAuthenticated: (state) => !!state?.user?.clientId, // Returns true if logged in
    getUser: (state) => state.user, // Retrieve user data
  },

  actions: {
    login(userData: User) {
      this.setUser(userData);
    },

    setUser(userData: User) {
      this.user = userData;
    },

    logout() {
      this.user = null;
      navigateTo('/'); // Redirect to login page
    },

    initializeAuth() {
      // retrieve session if needed
    },
  },
});
