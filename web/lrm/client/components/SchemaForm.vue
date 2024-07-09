<template>
  <div>
    <examples-list :examples="examples" @selected-example="load" />
    <RequestForm
      v-if="schema"
      :key="exampleLoadDatetime"
      v-model="form"
      :schema="schema"
      :no-send-button="noSendButton"
      @submit="submit()"
    />
  </div>
</template>

<script>
import mixinMessage from '~/mixins/mixinMessage'

export default {
  mixins: [mixinMessage],
  props: {
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
  },
  emits: ['on-form-update', 'modelValue'],
  data () {
    return {
      exampleLoadDatetime: undefined,
      form: {}
    }
  },
  watch: {
    form: {
      handler (newValue) {
        this.$emit('on-form-update', newValue) // Emit the 'on-form-update' event when the form is changed
      },
      deep: true
    }
  },
  methods: {
    load (example) {
      this.form = example
      // Trigger RequestForm reload with key change | Ref.: https://stackoverflow.com/a/48755228
      this.exampleLoadDatetime = new Date().toISOString()
      this.$emit('on-form-update', this.form) // Emit the 'on-form-update' event with the updated form
    },
    submit () {
      try {
        // const data = await (await fetch('samuA_to_samuB.json')).json()
        const data = this.buildMessage({
          [this.schema.title]: this.form
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
