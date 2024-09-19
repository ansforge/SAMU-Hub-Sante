<template>
  <div>
    <v-window-item v-for="messageTypeDetails in store.messageTypes" :key="messageTypeDetails.label">
      <examples-list ref="examplesListRef" :source="source" :examples="messageTypeDetails.examples" />
    </v-window-item>
    <RequestForm
      v-if="schema"
      ref="requestFormRef"
      :key="exampleLoadDatetime"
      :schema="schema"
      :no-send-button="noSendButton"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useMainStore } from '~/store'
const store = useMainStore()

const props = defineProps({
  currentMessageType: {
    type: Object,
    required: true
  },
  messageTypeTabIndex: {
    type: Number,
    required: true
  },
  source: {
    type: String,
    required: true
  },
  label: {
    type: String,
    required: true
  },
  schemaName: {
    type: String,
    required: true
  },
  examples: {
    type: Array,
    required: true
  },
  noSendButton: {
    type: Boolean,
    default: false
  }
})

const requestFormRef = ref(null)
const examplesListRef = ref(null)
const exampleLoadDatetime = ref(undefined)
let schema = reactive({})

watch(() => props.messageTypeTabIndex, () => {
  constructSchema()
  store.currentUseCase = store.messageTypes[props.messageTypeTabIndex].schema.title
})

function constructSchema () {
  schema = store.messageTypes[props.messageTypeTabIndex].schema
}

defineExpose({
  props,
  constructSchema,
  requestFormRef
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
