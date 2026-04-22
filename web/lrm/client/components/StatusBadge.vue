<template>
  <v-badge :color="getBadgeColor()" :content="getBadgeWording()" />
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
    getBadgeColor() {
      if (!this.acked) return 'orange';
      if (this.isRefused()) return 'red';
      return 'green';
    },
    getBadgeWording() {
      if (!this.acked) {
        return this.direction === DIRECTIONS.OUT ? 'En envoi' : 'Délivré';
      }
      return this.isRefused() ? 'Refusé' : 'Acquitté';
    },
    isRefused() {
      return (
        this.acked?.body?.content?.[0]?.jsonContent?.embeddedJsonContent
          ?.message?.reference?.refused === true
      );
    },
  },
};
</script>
