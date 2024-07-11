<template>
  <v-form v-model="valid">
    <v-card-actions height="100%">
      <v-spacer />
      <SendButton v-if="!noSendButton" @click="$emit('submit')" />
    </v-card-actions>
    <vjsf v-model="form" :schema="remove$PropsFromSchema(schemaCopy)" :options="options" />
  </v-form>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import Vjsf from '@koumoul/vjsf'
import moment from 'moment'
import { useMainStore } from '~/store'

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

const emit = defineEmits(['submit'])

const store = useMainStore()
const form = ref({})
const valid = ref(false)
const options = ref({
  locale: 'fr',
  defaultLocale: 'fr',
  rootDisplay: 'tabs',
  editMode: 'inline',
  expansionPanelsProps: { mandatory: false },
  density: 'compact',
  updateOn: 'blur',
  formats: {
    'date-time': (dateTime, _locale) => moment(new Date(dateTime)).format()
  }
})

const schemaCopy = computed(() => JSON.parse(JSON.stringify(props.schema)))

const remove$PropsFromSchema = (schema) => {
  const newSchema = { ...schema }
  delete newSchema.$schema
  delete newSchema.$id
  return newSchema
}

const updateStore = () => {
  store.currentMessage = form.value
}

watch(form, () => {
  updateStore()
})

onMounted(() => {
  form.value = store.currentMessage
})

const updateForm = () => {
  form.value = store.currentMessage
}

defineExpose({
  updateForm
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
