<template>
  <v-btn
    :color="messageJustSent ? 'secondary' : 'primary'"
    :disabled="(!isAdvanced) && messageJustSent"
    v-on="$listeners"
  >
    <v-icon left>
      {{ messageJustSent ? 'mdi-check' : 'mdi-send' }}
    </v-icon>
    {{ messageJustSent ? 'Envoyé !' : 'Envoyer' }}
  </v-btn>
</template>

<script>
//import { mapGetters } from 'vuex'

export default {
  computed: {
    ...mapGetters(['messageJustSent', 'isAdvanced'])
  }
}
</script>
