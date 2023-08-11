<template>
  <div>
    <examples-list :examples="examples" @selectedExample="load" />
    <RequestForm
      v-if="schema"
      :key="exampleLoadDatetime"
      v-model="form"
      :schema="schema"
      @submit="submit(null)"
    />
  </div>
</template>

<script>
import { v4 as uuidv4 } from 'uuid'
import moment from 'moment/moment'
import { WRONG_EDXL_ENVELOPE, DIRECTIONS, EDXL_ENVELOPE } from '@/constants'

export default {
  props: {
    name: {
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
    }
  },
  data () {
    return {
      exampleLoadDatetime: undefined,
      form: {}
    }
  },
  methods: {
    load (example) {
      this.form = example
      // Trigger RequestForm reload with key change | Ref.: https://stackoverflow.com/a/48755228
      this.exampleLoadDatetime = new Date().toISOString()
    },
    time () {
      const d = new Date()
      return d.toLocaleTimeString().replace(':', 'h') + '.' + d.getMilliseconds()
    },
    submit (request) {
      const time = this.time()
      // const data = await (await fetch('samuA_to_samuB.json')).json()
      const data = this.buildMessage()
      console.log('submit', request, data)
      // Could be done using Swagger generated client, but it would validate fields!
      this.$axios.$post(
        '/publish',
        { key: this.user.clientId, msg: data },
        { timeout: 1000 }
      ).then(() => {
        this.$emit('sent', {
          direction: DIRECTIONS.OUT,
          routingKey: this.user.targetId,
          time,
          code: 200,
          body: data
        })
      }).catch((error) => {
        console.error("Erreur lors de l'envoi du message", error)
      })
    },
    buildMessage () {
      return this.buildWrongMessage()
      // ToDo: remove above line once messages are built with the correct full EDXL envelope
      const message = JSON.parse(JSON.stringify(EDXL_ENVELOPE)) // Deep copy
      message.content.contentObject.jsonContent.embeddedJsonContent.message.createEvent = this.form
      const name = this.userInfos.name
      const messageId = uuidv4()
      const targetId = this.clientInfos(this.user.targetId).id
      const sentAt = moment().format()
      message.distributionID = `${name}_${messageId}`
      message.senderID = this.user.clientId
      message.dateTimeSent = sentAt
      message.descriptor.explicitAddress.explicitAddressValue = targetId
      message.content.contentObject.jsonContent.embeddedJsonContent.message.messageId = messageId
      message.content.contentObject.jsonContent.embeddedJsonContent.message.sender = { name, uri: `hubsante:${this.user.clientId}}` }
      message.content.contentObject.jsonContent.embeddedJsonContent.message.sentAt = sentAt
      message.content.contentObject.jsonContent.embeddedJsonContent.message.recipients.recipient = [{ name: this.clientInfos(this.user.targetId).name, uri: `hubsante:${targetId}}` }]
      return message
    },
    buildWrongMessage () {
      const message = JSON.parse(JSON.stringify(WRONG_EDXL_ENVELOPE)) // Deep copy
      message.content.contentObject.jsonContent.embeddedJsonContent.message = this.form
      const name = this.userInfos.name
      const messageId = uuidv4()
      const targetId = this.clientInfos(this.user.targetId).id
      const sentAt = moment().format()
      message.distributionID = `${name}_${messageId}`
      message.senderID = this.user.clientId
      message.dateTimeSent = sentAt
      message.descriptor.explicitAddress.explicitAddressValue = targetId
      return message
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
