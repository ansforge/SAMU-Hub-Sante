<template>
  <v-btn
    :color="store.messageJustSent ? 'secondary' : 'primary'"
    :disabled="(!store.isAdvanced) && store.messageJustSent"
  >
    <v-icon start>
      {{ store.messageJustSent ? 'mdi-check' : 'mdi-send' }}
    </v-icon>
    {{ store.messageJustSent ? 'Envoyé !' : 'Envoyer' }}
  </v-btn>
</template>

<script>
import { useMainStore } from '~/store'

export default {
  data: () => ({
    store: useMainStore()
  }),
  computed: {
  }
}
</script>
