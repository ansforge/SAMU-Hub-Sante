import Vue from 'vue'
import { mapGetters } from 'vuex'

Vue.mixin({
  computed: {
    ...mapGetters(['user', 'isAuthenticated', 'isAdvanced', 'showSentMessages', 'autoAck']),
    userInfos () {
      if (this.isAuthenticated) {
        return this.clientInfos(this.user.clientId)
      }
      return {}
    }
  },
  methods: {
    clientInfos (clientId) {
      return {
        name: clientId.split('.').splice(2).join('.'), // Remove the first two parts of the clientId (ex: fr.health)
        icon: clientId.split('.')[1] === 'health' ? 'mdi-heart-pulse' : 'mdi-fire',
        id: clientId.split('.').slice(0, 3).join('.')
      }
    }
  }
})
