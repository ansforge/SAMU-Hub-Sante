<template>
  <div>
    <v-chip-group
      v-model="selectedDetail"
      active-class="deep-purple--text text--accent-4"
    >
      <v-chip v-for="{icon, name} in examples" :key="name">
        <v-icon left>
          {{ icon }}
        </v-icon>
        {{ name }}
      </v-chip>
    </v-chip-group>
  </div>
</template>

<script>
export default {
  props: {
    examples: {
      type: Array,
      required: true
    }
  },
  data () {
    return {
      selectedDetail: null
    }
  },
  watch: {
    form () {
      this.$emit('input', this.form)
    }
  }
}
</script>
