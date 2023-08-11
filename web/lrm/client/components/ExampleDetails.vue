<template>
  <v-card
    class="mx-auto"
    outlined
    tile
    elevation="4"
  >
    <v-list-item two-line>
      <v-list-item-avatar
        size="64"
        color="grey"
      >
        <v-icon large>
          {{ icon }}
        </v-icon>
      </v-list-item-avatar>
      <v-list-item-content>
        <v-list-item-title class="text-h5 mb-1">
          {{ name }}
        </v-list-item-title>
        <v-list-item-subtitle>{{ context }}</v-list-item-subtitle>
      </v-list-item-content>
      <v-spacer />
      <v-btn
        icon
        color="primary"
        :href="'examples/' + file"
        target="_blank"
      >
        <v-icon>mdi-open-in-new</v-icon>
      </v-btn>
    </v-list-item>
    <v-card-text class="pt-0">
      <div class="py-1">
        <v-icon>mdi-phone</v-icon>
        {{ caller }}
      </div>
      <div class="py-1">
        <v-icon>mdi-map-marker</v-icon>
        {{ environment }}
      </div>
      <div class="py-1">
        <v-icon>mdi-account-multiple</v-icon>
        {{ victims }}
      </div>
      <div class="py-1">
        <v-icon>mdi-account-injury</v-icon>
        {{ victim }}
      </div>
      <div class="py-1">
        <v-icon>mdi-heart-pulse</v-icon>
        {{ medicalSituation }}
      </div>
    </v-card-text>
  </v-card>
</template>

<script>
export default {
  props: {
    file: {
      type: String,
      required: true
    },
    icon: {
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
