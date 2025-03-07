import { useAuthStore } from '~/store/auth';

export function clientInfos(clientId = null) {
  if (!clientId) {
    const authStore = useAuthStore();
    clientId = authStore.user.clientId;
  }
  return {
    name: clientId?.split('.').splice(2).join('.'), // Remove the first two parts of the clientId (ex: fr.health)
    icon: clientId?.split('.')[1] === 'health' ? 'mdi-heart-pulse' : 'mdi-fire',
  };
}
