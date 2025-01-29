import { useMainStore } from '~/store';

export default {
  data() {
    return {
      store: useMainStore(),
    };
  },
  computed: {
    userInfos() {
      if (this.store.isAuthenticated) {
        return this.clientInfos(this.store.user.clientId);
      }
      return {};
    },
  },
  methods: {
    clientInfos(clientId) {
      return {
        name: clientId.split('.').splice(2).join('.'), // Remove the first two parts of the clientId (ex: fr.health)
        icon:
          clientId.split('.')[1] === 'health' ? 'mdi-heart-pulse' : 'mdi-fire',
      };
    },
  },
};
