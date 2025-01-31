<template>
  <v-btn
    :color="store.messageJustSent ? 'secondary' : 'primary'"
    :disabled="!store.isAdvanced && store.messageJustSent"
    variant="flat"
  >
    <v-icon start>
      {{ store.messageJustSent ? 'mdi-check' : 'mdi-send' }}
    </v-icon>
    {{ store.messageJustSent ? 'Envoyé !' : 'Envoyer' }}
  </v-btn>
</template>

<script>
  import { useMainStore } from '~/store';
  import { useAuthStore } from '@/store/auth';

  export default {
    data: () => ({
      store: useMainStore(),
      authStore: useAuthStore(),
    }),
    computed: {},
  };
</script>
