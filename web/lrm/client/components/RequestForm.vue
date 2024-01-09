<template>
  <v-form v-model="valid">
    <v-jsf v-model="form" :schema="schemaCopy" :options="options" />
    <v-card-actions>
      <v-spacer />
      <SendButton v-if="!noSendButton" class="mt-2" @click="$emit('submit')" />
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
    },
    noSendButton: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      valid: false,
      // Passed through v-model
      form: this.value,
      // Super tricky: schema deep-copy required as VJSF updates it
      // But it is in Vuex store thus it can't be changed outside of mutations...
      schemaCopy: JSON.parse(JSON.stringify(this.schema)),
      options: {
        locale: 'fr',
        defaultLocale: 'fr',
        rootDisplay: 'tabs',
        editMode: 'inline', // edits in place and not in dialog
        expansionPanelsProps: { mandatory: false }, // collapses all panels
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
