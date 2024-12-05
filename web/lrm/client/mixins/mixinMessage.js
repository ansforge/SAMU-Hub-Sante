import { consola } from 'consola'
import { DIRECTIONS } from '@/constants'
import mixinUser from '~/mixins/mixinUser'
import { useMainStore } from '~/store'
import { buildAck, sendMessage } from '~/composables/messageUtils'
const VALUES_TO_DROP = [null, undefined, '']

export default {
  mixins: [mixinUser],
  data: () => ({
    store: useMainStore()
  }),
  mounted () {
    if (this.store.socket === null && this.$route.name !== 'json') {
      this.wsConnect()
    }
  },
  beforeRouteLeave (to, from, next) {
    this.wsDisconnect()
    next()
  },
  methods: {
    wsConnect () {
      this.store.socket = new WebSocket('wss://' + this.$config.public.backendLrmServer + '/lrm/api/')
      this.store.socket.onopen = () => {
        consola.log(`WebSocket ${this.$options.name} connection established`)
      }

      this.store.socket.onclose = (e) => {
        // Prevents infinite loop when closing the connection in an expected way
        if (this.disconnect) {
          return
        }
        consola.log(`WebSocket ${this.$options.name} connection closed`, e)
        // Retry connection
        setTimeout(() => {
          this.wsConnect()
        }, 1000)
      }

      this.store.socket.onerror = (err) => {
        consola.error(`WebSocket ${this.$options.name} connection errored`, err)
        this.store.socket.close()
      }

      // demo.vue is in charge of listening to server messages
      if (this.$options.name === 'Demo' || this.$options.name === 'Testcase') {
        this.store.socket.addEventListener('message', (event) => {
          const message = JSON.parse(event.data)
          this.store.addMessage({
            ...message,
            direction: DIRECTIONS.IN,
            messageType: this.getReadableMessageType(message.body.distributionKind),
            receivedTime: this.timeDisplayFormat()
          })
          if (this.autoAck) {
          // Send back acks automatically to received messages
            if (this.getMessageType(message) !== 'ack' && message.routingKey.startsWith(this.store.user.clientId)) {
              const msg = buildAck(message.body.distributionID)
              sendMessage(msg)
            }
          }
        })
      }
    },
    wsDisconnect () {
      if (this.store.socket) {
        consola.log(`Disconnecting: WebSocket ${this.$options.name} connection closed`)
        this.store.socket.close()
      }
      this.disconnect = true
    },
    isOut (direction) {
      return direction === DIRECTIONS.OUT
    },
    /**
     * Gets the case id from the message whether it's RC-EDA, EMSI, RS-EDA or RS-EDA-SMUR
     @param {*} message the message to retrieve the caseId from
     @param {*} isRootMessage True if complete message is provided. Else content from message.body.content[0].jsonContent.embeddedJsonContent is expected
     * */
    getCaseId (message, isRootMessage = false) {
      if (isRootMessage) {
        message = message.body.content[0].jsonContent.embeddedJsonContent.message
      }
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
     @param {*} message
     @param {*} caseId
     @param {*} localCaseId
     * */
    setCaseId (message, caseId, localCaseId) {
      switch (this.getMessageKind(message)) {
        case 'RC-EDA':
          message.createCase.caseId = caseId
          message.createCase.senderCaseId = localCaseId
          break
        case 'EMSI':
          message.emsi.EVENT.MAIN_EVENT_ID = caseId
          message.emsi.EVENT.ID = localCaseId
          break
        case 'RS-EDA':
          message.createCaseHealth.caseId = caseId
          message.createCaseHealth.senderCaseId = localCaseId
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
     * Returns a string representing message type (RC-EDA, EMSI, RS-EDA ou RS-EDA-SMUR)
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
    isEmpty (obj) {
      if (typeof obj === 'object') {
        return Object.keys(obj).length === 0
      }
      return false
    },
    trimEmptyValues (obj) {
      return Object.entries(obj).reduce((acc, [key, value]) => {
        if (!(VALUES_TO_DROP.includes(value) || this.isEmpty(value))) {
          if (typeof value !== 'object') {
            acc[key] = value
          } else {
            value = this.trimEmptyValues(value)
            if (!this.isEmpty(value)) {
              acc[key] = value
            }
          }
        }
        return Array.isArray(obj) ? Object.values(acc) : acc
      }, {})
    },
    formatIdsInMessage (innerMessage) {
      // Check the entire message for occurences of {senderName} and replace it with the actual sender name
      const senderName = this.userInfos.name
      let jsonString = JSON.stringify(innerMessage)
      jsonString = jsonString.replaceAll('samu690', senderName)
      return JSON.parse(jsonString)
    },
    timeDisplayFormat () {
      const d = new Date()
      return d.toLocaleTimeString('fr').replace(':', 'h') + '.' + String(new Date().getMilliseconds()).padStart(3, '0')
    }
  }
}
