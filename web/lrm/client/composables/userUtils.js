import { useMainStore } from '~/store'

const store = useMainStore()

export function clientInfos (clientId = store.user.clientId) {
  return {
    name: clientId.split('.').splice(2).join('.'), // Remove the first two parts of the clientId (ex: fr.health)
    icon: clientId.split('.')[1] === 'health' ? 'mdi-heart-pulse' : 'mdi-fire'
  }
}
