<template>
  <v-card v-if="!showDetails">
    <v-chip prepend-icon="$vuetify">
      Chip
    </v-chip>
  </v-card>
  <v-card v-else>
    Test
  </v-card>
</template>

<script>
export default {
  props: {
    file: {
      type: String,
      required: true
    },
    name: {
      type: String,
      required: true
    },
    caller: {
      type: String,
      required: true
    },
    context: {
      type: String,
      required: true
    },
    environment: {
      type: String,
      required: true
    },
    victims: {
      type: String,
      required: true
    },
    victim: {
      type: String,
      required: true
    },
    medicalSituation: {
      type: String,
      required: true
    }
  },
  data () {
    return {
      showDetails: false
    }
  },
  watch: {
    form () {
      this.$emit('input', this.form)
    }
  }
}
</script>
