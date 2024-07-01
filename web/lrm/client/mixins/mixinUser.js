import { defineComponent, computed } from 'vue'
import { useMainStore } from '~/store'

export default defineComponent({
  setup() {
    const store = useMainStore()
    
    // Map Vuex getters to computed properties
    const user = computed(() => store.getters['user'])
    const isAuthenticated = computed(() => store.getters['isAuthenticated'])
    const isAdvanced = computed(() => store.getters['isAdvanced'])
    const showSentMessages = computed(() => store.getters['showSentMessages'])
    const autoAck = computed(() => store.getters['autoAck'])

    // Methods
    const clientInfos = (clientId) => {
      return {
        name: clientId.split('.').splice(2).join('.'), // Remove the first two parts of the clientId (ex: fr.health)
        icon: clientId.split('.')[1] === 'health' ? 'mdi-heart-pulse' : 'mdi-fire'
      }
    }

    // Computed property for userInfos
    const userInfos = computed(() => {
      if (isAuthenticated.value) {
        return clientInfos(user.value.clientId)
      }
      return {}
    })

    return {
      user,
      isAuthenticated,
      isAdvanced,
      showSentMessages,
      autoAck,
      userInfos,
      clientInfos
    }
  }
})