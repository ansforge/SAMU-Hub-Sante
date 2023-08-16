import Vue from 'vue'
import { mapGetters } from 'vuex'

Vue.mixin({
  computed: {
    ...mapGetters(['user', 'isAuthenticated', 'isAdvanced']),
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
        name: clientId.split('.')[2],
        icon: clientId.split('.')[1] === 'health' ? 'mdi-heart-pulse' : 'mdi-fire',
        id: clientId.split('.').slice(0, 3).join('.')
      }
    }
  }
})
