import { useAuthStore } from '@/store/auth';

export default {
  data() {
    return {
      authStore: useAuthStore(),
    };
  },
  computed: {
    userInfos() {
      if (this.authStore.isAuthenticated) {
        return this.clientInfos(this.authStore.user.clientId);
      }
      return {};
    },
  },
  methods: {
    clientInfos(clientId) {
      return {
        name: clientId?.split('.').splice(2).join('.'), // Remove the first two parts of the clientId (ex: fr.health)
        icon:
          clientId?.split('.')[1] === 'health' ? 'mdi-heart-pulse' : 'mdi-fire',
      };
    },
  },
};
