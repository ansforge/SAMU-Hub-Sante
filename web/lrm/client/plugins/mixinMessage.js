import { v4 as uuidv4 } from 'uuid'
import moment from 'moment/moment'
import { EDXL_ENVELOPE, DIRECTIONS } from '@/constants'

export default {
  mounted () {
    if (this.$route.name !== 'json') {
      this.wsConnect()
    }
  },
  beforeRouteLeave (to, from, next) {
    this.wsDisconnect()
    next()
  },
  methods: {
    wsConnect () {
      this.socket = new WebSocket(process.env.wssUrl)
      this.socket.onopen = () => {
        console.log(`WebSocket ${this.$options.name} connection established`)
      }

      this.socket.onclose = (e) => {
        // Prevents infinite loop when closing the connection in an expected way
        if (this.disconnect) {
          return
        }
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
      if (this.$options.name === 'Demo' || this.$options.name === 'Testcase') {
        this.socket.addEventListener('message', (event) => {
          const message = JSON.parse(event.data)
          this.$store.dispatch('addMessage', {
            ...message,
            direction: DIRECTIONS.IN,
            messageType: this.getReadableMessageType(message.body.distributionKind),
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
    wsDisconnect () {
      if (this.socket) {
        console.log(`Disconnecting: WebSocket ${this.$options.name} connection closed`)
        this.socket.close()
      }
      this.disconnect = true
    },
    isOut (direction) {
      return direction === DIRECTIONS.OUT
    },
    getCaseId (message) {
      return message.body.content[0].jsonContent.embeddedJsonContent.message.caseId
    },
    /**
     * Gets the case id from the message whether it's RC-EDA, EMSI or RS-EDA
     * @param {*} message at the path message.body.content[0].jsonContent.embeddedJsonContent instead of root-level
     */
    getCaseIdOfAnyKind (message) {
      switch (this.getMessageKind(message)) {
        case 'RC-EDA':
          return message.createCase.caseId
        case 'EMSI':
          return message.emsi.EVENT.MAIN_EVENT_ID
        case 'RS-EDA':
          return message.createCaseHealth.caseId
      }
    },
    /**
     * Sets the case ID in the message to the current case ID
     * @param {*} message
     */
    setCaseId (message) {
      switch (this.getMessageKind(message)) {
        case 'RC-EDA':
          message.createCase.caseId = this.currentCaseId
          message.createCase.senderCaseId = this.localCaseId
          break
        case 'EMSI':
          message.emsi.EVENT.MAIN_EVENT_ID = this.currentCaseId
          message.emsi.EVENT.ID = this.localCaseId
          break
        case 'RS-EDA':
          message.createCaseHealth.caseId = this.currentCaseId
          message.createCaseHealth.senderCaseId = this.localCaseId
          break
      }
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
    /**
     * Returns a string representing message type (RC-EDA, EMSI ou RS-EDA)
     * @param {*} message
     */
    getMessageKind (message) {
      if (message.createCase) {
        return 'RC-EDA'
      } else if (message.emsi) {
        return 'EMSI'
      } else if (message.createCaseHealth) {
        return 'RS-EDA'
      }
    },
    /**
     * Replaces values in a message using jsonpath:value pairs
     */
    replaceValues (message, requiredValues) {
      const jp = require('jsonpath')
      requiredValues.forEach((entry) => {
        jp.value(message, entry.path, entry.value)
      })
      return message
    },
    getReadableMessageType (messageType) {
      switch (messageType) {
        case 'Ack':
          return 'Ack'
        case 'Error':
          return 'Info'
        default:
          return 'Message'
      }
    },
    buildAck (distributionID) {
      return this.buildMessage({ reference: { distributionID } }, 'Ack')
    },
    buildMessage (innerMessage, distributionKind = 'Report') {
      const message = JSON.parse(JSON.stringify(EDXL_ENVELOPE)) // Deep copy
      const formattedInnerMessage = this.formatIdsInMessage(innerMessage)
      message.content[0].jsonContent.embeddedJsonContent.message = {
        ...message.content[0].jsonContent.embeddedJsonContent.message,
        ...formattedInnerMessage
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
    formatIdsInMessage (innerMessage) {
      // Check the entire message for occurencesof {senderName} and replaceit with the actual sender name
      const senderName = this.userInfos.name
      let jsonString = JSON.stringify(innerMessage)
      jsonString = jsonString.replaceAll('samu690', senderName)
      return JSON.parse(jsonString)
    },
    timeDisplayFormat () {
      const d = new Date()
      return d.toLocaleTimeString('fr').replace(':', 'h') + '.' + String(new Date().getMilliseconds()).padStart(3, '0')
    },
    sendMessage (msg) {
      if (this.socket.readyState === 1) {
        try {
          console.log('Sending message', msg)
          this.socket.send(JSON.stringify({ key: this.user.clientId, msg }))
          this.$store.dispatch('addMessage', {
            direction: DIRECTIONS.OUT,
            routingKey: this.user.targetId,
            time: this.timeDisplayFormat(),
            messageType: this.getReadableMessageType(msg.distributionKind),
            body: msg
          })
        } catch (e) {
          alert(`Erreur lors de l'envoi du message: ${e}`)
        }
      } else {
        // TODO: Add proper retry logic here with either exponential backoff or a retry limit
        console.log('Socket is not open. Retrying in half a second.')
        setTimeout(() => {
          this.sendMessage(msg)
        }, 500)
      }
    }
  }
}
