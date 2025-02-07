<template>
  <v-badge
    v-if="isOut(direction)"
    :color="acked ? 'green' : 'orange'"
    :content="acked ? 'Acquitté' : 'En envoi'"
  />
  <v-badge
    v-else
    :color="acked ? 'green' : 'orange'"
    :content="acked ? 'Acquitté' : 'Délivré'"
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
  },
};
</script>
