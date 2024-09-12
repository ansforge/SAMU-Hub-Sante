<template>
  <div>
    <examples-list :source="source" :examples="examples" @example-loaded="refreshForm"/>
    <RequestForm
      v-if="schema"
      ref="requestFormRef"
      :key="exampleLoadDatetime"
      :schema="schema"
      :no-send-button="noSendButton"
      @submit="submit"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import mixinMessage from '~/mixins/mixinMessage'
import { useMainStore } from '~/store'
const store = useMainStore()

const props = defineProps({
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
  schema: {
    type: Object,
    default: null
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
const exampleLoadDatetime = ref(undefined)
const form = reactive({})

function refreshForm () {
  requestFormRef.value?.updateForm()
}

defineExpose({
  props
})
</script>

<script>
export default {
  mixins: [mixinMessage],
  methods: {
    submit (form) {
      try {
        // const data = await (await fetch('samuA_to_samuB.json')).json()
        const data = this.buildMessage({
          [this.schema.title]: form.form
        })
        this.sendMessage(data)
      } catch (error) {
        console.error("Erreur lors de l'envoi du message", error)
      }
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
