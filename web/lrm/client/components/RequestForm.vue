<template>
  <v-form v-model="valid">
    <v-jsf v-model="form" :schema="schema" :options="options" />
    <v-btn color="primary" @click="$emit('submit')">
      <v-icon left>
        mdi-send
      </v-icon>
      Envoyer
    </v-btn>
  </v-form>
</template>

<script>
export default {
  props: {
    value: {
      type: Object,
      default: () => ({})
    },
    schema: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      valid: false,
      // Passed through v-model
      form: this.value,
      options: {
        locale: 'fr',
        defaultLocale: 'fr',
        rootDisplay: 'expansion-panels',
        editMode: 'inline',
        expansionPanelsProps: { mandatory: false }
      }
    }
  },
  watch: {
    form () {
      this.$emit('input', this.form)
    }
  }
}
</script>

<style>
.v-application div.vjsf-array-header {
  margin-bottom: 28px !important;
}
.v-application div.vjsf-array {
  margin-bottom: 12px !important;
}
</style>
