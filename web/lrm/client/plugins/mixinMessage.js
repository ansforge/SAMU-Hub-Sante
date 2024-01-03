import { v4 as uuidv4 } from 'uuid'
import moment from 'moment/moment'
import { EDXL_ENVELOPE, DIRECTIONS } from '@/constants'

export default {
  mounted () {
    if (this.$route.name !== 'json') {
      this.wsConnect()
    }
  },
  methods: {
    wsConnect () {
      this.socket = new WebSocket(process.env.wssUrl)
      this.socket.onopen = () => {
        console.log(`WebSocket ${this.$options.name} connection established`)
      }

      this.socket.onclose = (e) => {
        console.log(`WebSocket ${this.$options.name} connection closed`, e)
        // Retry connection
        setTimeout(() => {
          this.wsConnect()
        }, 1000)
      }

      this.socket.onerror = (err) => {
        console.error(`WebSocket ${this.$options.name} connection errored`, err)
        this.socket.close()
      }

      // demo.vue is in charge of listening to server messages
      if (this.$options.name === 'Demo') {
        this.socket.addEventListener('message', (event) => {
          const message = JSON.parse(event.data)
          this.$store.dispatch('addMessage', {
            ...message,
            direction: DIRECTIONS.IN,
            receivedTime: this.timeDisplayFormat()
          })
          if (this.autoAck) {
          // Send back acks automatically to received messages
            if (this.getMessageType(message) !== 'ack' && message.routingKey.startsWith(this.user.clientId)) {
              const msg = this.buildAck(message.body.distributionID)
              this.sendMessage(msg)
            }
          }
        })
      }
    },
    isOut (direction) {
      return direction === DIRECTIONS.OUT
    },
    getCaseId (message) {
      return message.body.content[0].jsonContent.embeddedJsonContent.message.caseId
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
    buildAck (distributionID) {
      return this.buildMessage({ reference: { distributionID } }, 'Ack')
    },
    buildMessage (innerMessage, distributionKind = 'Report') {
      const message = JSON.parse(JSON.stringify(EDXL_ENVELOPE)) // Deep copy
      message.content[0].jsonContent.embeddedJsonContent.message = {
        ...message.content[0].jsonContent.embeddedJsonContent.message,
        ...innerMessage
      }
      const name = this.userInfos.name
      const targetId = this.clientInfos(this.user.targetId).id
      const sentAt = moment().format()
      message.distributionID = `${this.user.clientId}_${uuidv4()}`
      message.distributionKind = distributionKind
      message.senderID = this.user.clientId
      message.dateTimeSent = sentAt
      message.descriptor.explicitAddress.explicitAddressValue = targetId
      message.content[0].jsonContent.embeddedJsonContent.message.messageId = message.distributionID
      message.content[0].jsonContent.embeddedJsonContent.message.kind = message.distributionKind
      message.content[0].jsonContent.embeddedJsonContent.message.sender = { name, URI: `hubex:${this.user.clientId}` }
      message.content[0].jsonContent.embeddedJsonContent.message.sentAt = sentAt
      message.content[0].jsonContent.embeddedJsonContent.message.recipient = [{ name: this.clientInfos(this.user.targetId).name, URI: `hubex:${targetId}` }]
      return message
    },
    timeDisplayFormat () {
      const d = new Date()
      return d.toLocaleTimeString('fr').replace(':', 'h') + '.' + String(new Date().getMilliseconds()).padStart(3, '0')
    },
    sendMessage (msg) {
      try {
        console.log('Sending message', msg)
        this.socket.send(JSON.stringify({ key: this.user.clientId, msg }))
        this.$store.dispatch('addMessage', {
          direction: DIRECTIONS.OUT,
          routingKey: this.user.targetId,
          time: this.timeDisplayFormat(),
          body: msg
        })
      } catch (e) {
        alert(`Erreur lors de l'envoi du message: ${e}`)
      }
    }
  }
}
