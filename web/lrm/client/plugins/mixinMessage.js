import { v4 as uuidv4 } from 'uuid'
import moment from 'moment/moment'
import { WRONG_EDXL_ENVELOPE, EDXL_ENVELOPE, DIRECTIONS } from '@/constants'

export default {
  mounted () {
    this.socket = new WebSocket(process.env.wssUrl)
    this.socket.addEventListener('open', () => {
      console.log(`WebSocket ${this.$options.name} connection established`)
    })
    this.socket.addEventListener('close', () => {
      console.log(`WebSocket ${this.$options.name} connection closed`)
    })
  },
  methods: {
    isOut (direction) {
      return direction === DIRECTIONS.OUT
    },
    getMessageType (message) {
      if (message.body.distributionKind === 'Ack') {
        return 'ack'
      } else if (message.body.distributionKind === 'Error') {
        return 'info'
      } else {
        return 'message'
      }
    },
    buildMessage (innerMessage, distributionKind = 'Report') {
      return this.buildWrongMessage('createEvent' in innerMessage ? innerMessage.createEvent : innerMessage, distributionKind)
      // ToDo: remove above line once messages are built with the correct full EDXL envelope
      const message = JSON.parse(JSON.stringify(EDXL_ENVELOPE)) // Deep copy
      message.content.contentObject.jsonContent.embeddedJsonContent.message = innerMessage
      const name = this.userInfos.name
      const messageId = uuidv4()
      const targetId = this.clientInfos(this.user.targetId).id
      const sentAt = moment().format()
      message.distributionID = `${name}_${messageId}`
      message.distributionKind = distributionKind
      message.senderID = this.user.clientId
      message.dateTimeSent = sentAt
      message.descriptor.explicitAddress.explicitAddressValue = targetId
      message.content.contentObject.jsonContent.embeddedJsonContent.message.messageId = messageId
      message.content.contentObject.jsonContent.embeddedJsonContent.message.sender = { name, uri: `hubsante:${this.user.clientId}}` }
      message.content.contentObject.jsonContent.embeddedJsonContent.message.sentAt = sentAt
      message.content.contentObject.jsonContent.embeddedJsonContent.message.recipients.recipient = [{ name: this.clientInfos(this.user.targetId).name, uri: `hubsante:${targetId}}` }]
      return message
    },
    buildWrongMessage (innerMessage, distributionKind = 'Report') {
      const message = JSON.parse(JSON.stringify(WRONG_EDXL_ENVELOPE)) // Deep copy
      message.content.contentObject.jsonContent.embeddedJsonContent.message = innerMessage
      const name = this.userInfos.name
      const messageId = uuidv4()
      const targetId = this.clientInfos(this.user.targetId).id
      const sentAt = moment().format()
      message.distributionID = `${name}_${messageId}`
      message.distributionKind = distributionKind
      message.senderID = this.user.clientId
      message.dateTimeSent = sentAt
      message.descriptor.explicitAddress.explicitAddressValue = targetId
      return message
    },
    timeDisplayFormat () {
      const d = new Date()
      return d.toLocaleTimeString('fr').replace(':', 'h') + '.' + String(new Date().getMilliseconds()).padStart(3, '0')
    },
    sendMessage (msg) {
      console.log('Sending message', msg)
      this.socket.send(JSON.stringify({ key: this.user.clientId, msg }))
      this.$store.dispatch('addMessage', {
        direction: DIRECTIONS.OUT,
        routingKey: this.user.targetId,
        time: this.timeDisplayFormat(),
        body: msg
      })
    }
  }
}
