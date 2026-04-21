<template>
  <v-badge
    v-if="isOut(direction)"
    :color="acked ? (isRefused() ? 'red' : 'green') : 'orange'"
    :content="acked ? (isRefused() ? 'Refusé' : 'Acquitté') : 'En envoi'"
  />
  <v-badge
    v-else
    :color="acked ? (isRefused() ? 'red' : 'green') : 'orange'"
    :content="acked ? (isRefused() ? 'Refusé' : 'Acquitté') : 'Délivré'"
  />
</template>

<script>
import { DIRECTIONS } from '@/constants';

export default {
  props: {
    direction: {
      type: String,
      required: true,
    },
    acked: {
      type: Object,
      default: null,
    },
  },
  data() {
    return {
      DIRECTIONS,
    };
  },
  methods: {
    isOut(direction) {
      return direction === DIRECTIONS.OUT;
    },
    isRefused() {
      return (
        this.acked?.body?.content?.[0]?.jsonContent?.embeddedJsonContent?.message
        ?.reference?.refused === true
      );
      }
  },
};
</script>
