<template>
  <v-row justify="center">
    <v-col cols="12" md="7">
      <v-card style="height: 86vh; overflow-y: auto;">
        <v-card-title class="headline">
          Formulaire
          <v-spacer />
          <v-btn
            color="primary"
            @click="load('ExempleDubois.json')"
          >
            <v-icon left>
              mdi-upload
            </v-icon>
            Charger
          </v-btn>
        </v-card-title>
        <v-card-text>
          <v-alert type="info" dense class="mb-0">
            Ce formulaire permet d'envoyer des messages via le Hub Santé
          </v-alert>
          <RequestForm
            v-if="schema"
            :key="selectedExample"
            v-model="form"
            :schema="schema"
            @submit="submit(null)"
          />
        </v-card-text>
      </v-card>
    </v-col>
    <v-col cols="12" md="5">
      <v-card style="height: 86vh; overflow-y: auto;">
        <v-card-title class="headline">
          Messages
          <v-badge
            v-if="showedMessages.length"
            :content="showedMessages.length"
          />
          <v-spacer />
          <v-switch
            v-model="config.showSentMessages"
            inset
            :label="'Show sent (' + messagesSentCount + ')'"
          />
        </v-card-title>
        <v-btn-toggle
          v-model="selectedClientId"
          class="ml-4"
          dense
          borderless
          mandatory
        >
          <v-btn v-for="{name, type, icon} in messageTypes" :key="name" :value="name">
            <v-icon left>
              {{ icon }}
            </v-icon>
            {{ name }}
            <v-badge
              v-if="typeMessages(type).length > 0"
              class="mr-4 ml-1"
              :content="typeMessages(type).length"
            />
          </v-btn>
        </v-btn-toggle>
        <v-card-text>
          <transition-group name="message">
            <div
              v-for="{direction, routingKey, time, receivedTime, code, body} in showedMessages"
              :key="time"
              class="message mb-4"
            >
              <v-badge :color="code === 200 ? 'green' : 'red'" :content="code" />
              <pre
                style="white-space: pre-wrap; background-color: rgba(0, 0, 0, 0.05);"
                class="elevation-1 pa-2 mt-n3"
              ><span v-if="isOut(direction)">{{ direction }} {{
                routingKey
              }}</span><span v-else>{{ direction }} {{
                body.senderID
              }}</span><br>{{ time }} -> {{ receivedTime }}<br>{{
                  body
              }}</pre>
            </div>
          </transition-group>
        </v-card-text>
      </v-card>
    </v-col>
  </v-row>
</template>

<script>
import { v4 as uuidv4 } from 'uuid'
import moment from 'moment'
import { mapGetters } from 'vuex'

const DIRECTIONS = {
  IN: '←',
  OUT: '→'
}
const WRONG_EDXL_ENVELOPE = {
  distributionID: '{{ samuA_2608323d-507d-4cbf-bf74-52007f8124ea }}',
  senderID: '{{ fr.health.samuA }}',
  dateTimeSent: '{{ 2022-09-27T08:23:34+02:00 }}',
  dateTimeExpires: '2072-09-27T08:23:34+02:00',
  distributionStatus: 'Actual',
  distributionKind: 'Report',
  descriptor: {
    language: 'fr-FR',
    explicitAddress: {
      explicitAddressScheme: 'hubsante',
      explicitAddressValue: '{{ fr.health.samuB }}'
    }
  },
  content: {
    contentObject: {
      jsonContent: {
        embeddedJsonContent: {
          message: {}
        }
      }
    }
  }
}
const EDXL_ENVELOPE = {
  distributionID: '{{ samuA_2608323d-507d-4cbf-bf74-52007f8124ea }}',
  senderID: '{{ fr.health.samuA }}',
  dateTimeSent: '{{ 2022-09-27T08:23:34+02:00 }}',
  dateTimeExpires: '2072-09-27T08:23:34+02:00',
  distributionStatus: 'Actual',
  distributionKind: 'Report',
  descriptor: {
    language: 'fr-FR',
    explicitAddress: {
      explicitAddressScheme: 'hubsante',
      explicitAddressValue: '{{ fr.health.samuB }}'
    }
  },
  content: {
    contentObject: {
      jsonContent: {
        embeddedJsonContent: {
          message: {
            messageId: '{{ 2608323d-507d-4cbf-bf74-52007f8124ea }}',
            sender: {
              name: '{{ samuA }}',
              uri: '{{ hubsante:fr.health.samuA }}'
            },
            sentAt: '{{ 2022-09-27T08:23:34+02:00 }}',
            msgType: 'ALERT',
            status: 'TEST',
            recipients: {
              recipient: [
                {
                  name: '{{ samuB }}',
                  uri: '{{ hubsante:fr.health.samuB }}'
                }
              ]
            }
          }
        }
      }
    }
  }
}
export default {
  name: 'LRM',
  data () {
    return {
      schema: null,
      selectedExample: null,
      messages: [/* {
        direction: DIRECTIONS.IN,
        routingKey: '',
        time: this.time(),
        receivedTime: this.time(),
        code: 200,
        body: { body: 'Page loaded successfully!' }
      } */],
      config: {
        showSentMessages: false
      },
      selectedClientId: null,
      items: {
        clientId: ['fr.health.samuA', 'fr.health.samuB', 'fr.fire.nexsis.sdisZ'],
        routingKey: ['fr.health.samuA', 'fr.health.samuB', 'fr.fire.nexsis.sdisZ']
      },
      messageTypes: [{
        name: 'Message',
        type: 'message',
        icon: 'mdi-message'
      }, {
        name: 'Info',
        type: 'info',
        icon: 'mdi-information'
      }, {
        name: 'Ack',
        type: 'ack',
        icon: 'mdi-check'
      }],
      form: {}
    }
  },
  head () {
    return {
      title: 'Interface SI-SAMU tester'
    }
  },
  computed: {
    ...mapGetters(['user']),
    clientMessages () {
      return this.messages.filter(
        message => (
          (this.isOut(message.direction) && message.body.senderID === this.user.clientId) ||
            (!this.isOut(message.direction) && message.routingKey.startsWith(this.user.clientId))
        )
      )
    },
    showedMessages () {
      return this.config.showSentMessages ? this.clientMessages : this.clientMessages.filter(message => !this.isOut(message.direction))
    },
    messagesSentCount () {
      return this.clientMessages.filter(message => this.isOut(message.direction)).length
    }
  },
  mounted () {
    // To automatically generate the UI and input fields based on the JSON Schema
    fetch('schema.json')
      .then(response => response.json())
      .then((schema) => {
        this.schema = schema
        // For test purposes
        this.load('ExempleDubois.json')
      })
    // Start listening to server events
    this.longPolling()
  },
  methods: {
    longPolling () {
      this.$axios.$get('/poll', { timeout: 10000 }).then((response) => {
        this.messages.unshift({
          ...response,
          direction: DIRECTIONS.IN,
          receivedTime: this.time()
        })
        this.longPolling()
      }).catch((error) => {
        if (error.code === 'ECONNABORTED') {
          console.info('Long polling expiration, restarting.', error)
          this.longPolling()
        } else if (error.message === 'Network Error') {
          console.warn('Server unavailable, waiting before reconnection.', error)
          setTimeout(() => this.longPolling(), 300)
        } else {
          console.error('Error while reading message, restarting.', error)
          this.longPolling()
        }
      })
    },
    time () {
      const d = new Date()
      return d.toLocaleTimeString().replace(':', 'h') + '.' + d.getMilliseconds()
    },
    isOut (direction) {
      return direction === DIRECTIONS.OUT
    },
    typeMessages (type) {
      return this.showedMessages.filter((message) => {
        if (type === 'ack') {
          return message.body.distributionKind === 'Ack'
        } else if (type === 'info') {
          return message.body.distributionKind === 'Error'
        } else if (type === 'message') {
          return message.body.distributionKind !== 'Ack' && message.body.distributionKind !== 'Error'
        } else {
          console.error('Unexpected message type', type)
          return true
        }
      })
    },
    clientInfos (clientId) {
      return {
        name: clientId.split('.')[2],
        icon: clientId.split('.')[1] === 'health' ? 'mdi-heart-pulse' : 'mdi-fire',
        id: clientId.split('.').slice(0, 3).join('.')
      }
    },
    load (exampleName) {
      fetch('examples/' + exampleName)
        .then(response => response.json())
        .then((data) => {
          this.form = data
          // Trigger RequestForm reload with key change | Ref.: https://stackoverflow.com/a/48755228
          this.selectedExample = exampleName
        })
    },
    buildMessage () {
      return this.buildWrongMessage()
      const message = JSON.parse(JSON.stringify(EDXL_ENVELOPE)) // Deep copy
      message.content.contentObject.jsonContent.embeddedJsonContent.message.createEvent = this.form
      const name = this.clientInfos(this.user.clientId).name
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
      const name = this.clientInfos(this.user.clientId).name
      const messageId = uuidv4()
      const targetId = this.clientInfos(this.user.targetId).id
      const sentAt = moment().format()
      message.distributionID = `${name}_${messageId}`
      message.senderID = this.user.clientId
      message.dateTimeSent = sentAt
      message.descriptor.explicitAddress.explicitAddressValue = targetId
      return message
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
        this.messages.unshift({
          direction: DIRECTIONS.OUT,
          routingKey: this.user.targetId,
          time,
          code: 200,
          body: data
        })
      }).catch((error) => {
        console.error("Erreur lors de l'envoi du message", error)
      })
    }
  }
}
</script>
<style>
.message {
  transition: all 0.5s;
}

.message-enter, .message-leave-to
  /* .message-leave-active for <2.1.8 */
{
  opacity: 0;
  transform: scale(0.7) translateY(-500px);
}

.message-enter-to {
  opacity: 1;
  transform: scale(1);
}

.message-leave-active {
  /*position: absolute;*/
}

.message-move {
  opacity: 1;
  transition: all 0.5s;
}
</style>
