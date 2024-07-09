<template>
  <v-form v-model="valid">
    <v-card-actions height="100%">
      <v-spacer />
      <SendButton v-if="!noSendButton" @click="$emit('submit')" />
    </v-card-actions>
  </v-form>
  <vjsf v-model="form" :schema="remove$PropsFromSchema(schemaCopy)" :options="options" />
</template>

<script setup>
import Vjsf from '@koumoul/vjsf'
</script>

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
  emits: ['input', 'submit', 'modelValue'],
  data () {
    return {
      valid: false,
      // Passed through v-model
      form: this.value,
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
  computed: {
    // Super tricky: schema deep-copy required as VJSF updates it somehow
    // But it is in Vuex store thus it can't be changed outside of mutations...
    schemaCopy () {
      return JSON.parse(JSON.stringify(this.schema))
    }
  },
  watch: {
    form () {
      this.$emit('input', this.form)
    }
  },
  methods: {
    remove$PropsFromSchema (schema) {
    // Removes the $schema and $id props from schema object in order to not break vjsf
      const newSchema = { ...schema }
      delete newSchema.$schema
      delete newSchema.$id
      return newSchema
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
