<template>
  <v-form v-model="valid">
    <vjsf v-model="form" :schema="schemaCopy" :options="options" />
    <v-card-actions>
      <v-spacer />
      <SendButton v-if="!noSendButton" class="mt-2" @click="$emit('submit')" />
    </v-card-actions>
  </v-form>
</template>

<script setup>
import moment from 'moment'
import { ref, computed, watch } from 'vue'
import Vjsf from '@koumoul/vjsf'

const props = defineProps({
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
})

const valid = ref(false)
const form = ref(props.value)
const options = {
  locale: 'fr',
  defaultLocale: 'fr',
  rootDisplay: 'tabs',
  editMode: 'inline', // edits in place and not in dialog
  expansionPanelsProps: { mandatory: false }, // collapses all panels
  formats: {
    'date-time': function (dateTime, _locale) { return moment(new Date(dateTime)).format() }
  }
}

const schemaCopy = computed(() => {
  return JSON.parse(JSON.stringify(props.schema))
})

const emit = defineEmits(['input'])

watch(form, () => {
  emit('input', form.value)
})
</script>

<style>
.v-application div.vjsf-array-header {
  margin-bottom: 28px !important;
}
.v-application div.vjsf-array {
  margin-bottom: 12px !important;
}
</style>
