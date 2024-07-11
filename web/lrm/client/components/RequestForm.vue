<template>
  <v-form v-model="valid">
    <v-card-actions height="100%">
      <v-spacer />
      <SendButton v-if="!noSendButton" @click="$emit('submit')" />
    </v-card-actions>
    <vjsf v-model="store.currentMessage" :schema="remove$PropsFromSchema(schemaCopy)" :options="options" />
  </v-form>
</template>

<script setup>
import Vjsf from '@koumoul/vjsf'
import moment from 'moment'
import { useMainStore } from '~/store'
</script>

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
    },
    noSendButton: {
      type: Boolean,
      default: false
    }
  },
  emits: ['input', 'submit', 'modelValue'],
  data () {
    return {
      store: useMainStore(),
      valid: false,
      // Passed through v-model
      options: {
        locale: 'fr',
        defaultLocale: 'fr',
        rootDisplay: 'tabs',
        editMode: 'inline', // edits in place and not in dialog
        expansionPanelsProps: { mandatory: false }, // collapses all panels
        density: 'compact',
        updateOn: 'blur',
        formats: {
          'date-time': function (dateTime, _locale) { return moment(new Date(dateTime)).format() }
        }
      }
    }
  },
  computed: {
    schemaCopy () {
      return JSON.parse(JSON.stringify(this.schema))
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
