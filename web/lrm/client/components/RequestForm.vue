<template>
  <v-form v-model="valid">
    <v-jsf v-model="form" :schema="schema" :options="options" />
    <v-card-actions>
      <v-spacer />
      <v-btn color="primary" class="mt-2" @click="$emit('submit')">
        <v-icon left>
          mdi-send
        </v-icon>
        Envoyer
      </v-btn>
    </v-card-actions>
  </v-form>
</template>

<script>
import moment from 'moment'

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
        expansionPanelsProps: { mandatory: false },
        formats: {
          'date-time': function (dateTime, _locale) { return moment(new Date(dateTime)).format() }
        }
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
