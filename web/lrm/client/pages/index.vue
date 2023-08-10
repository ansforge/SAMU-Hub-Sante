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
          <v-card-title>Infos cœur</v-card-title>
          <v-form>
            <v-row>
              <v-col
                cols="12"
                md="6"
              >
                <v-combobox
                  v-model="header.clientId"
                  :items="items.clientId"
                  label="Identification"
                  hide-details="auto"
                  dense
                />
              </v-col>
              <v-col
                cols="12"
                md="6"
              >
                <v-combobox
                  v-model="header.routingKey"
                  :items="items.routingKey"
                  label="routingKey"
                  dense
                />
              </v-col>
            </v-row>
          </v-form>
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
          <v-btn v-for="clientId in items.clientId" :key="clientId" :value="clientId">
            <v-icon left>
              {{ clientInfos(clientId).icon }}
            </v-icon>
            <span class="hidden-sm-and-down">
              {{ clientInfos(clientId).name }}
              <v-badge
                v-if="clientMessages(clientId).length > 0"
                class="mr-4 ml-1"
                :content="clientMessages(clientId).length"
              />
            </span>
          </v-btn>
        </v-btn-toggle>
        <v-card-text>
          <transition-group name="message">
            <div
              v-for="{direction, routingKey, time, receivedTime, code, body} in selectedMessages"
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
  name: 'IndexPage',
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
      header: {
        clientId: 'fr.health.samuA',
        routingKey: 'fr.health.samuB'
      },
      form: {}
    }
  },
  head () {
    return {
      title: 'Interface SI-SAMU tester'
    }
  },
  computed: {
    showedMessages () {
      return this.config.showSentMessages ? this.messages : this.messages.filter(message => !this.isOut(message.direction))
    },
    messagesSentCount () {
      return this.messages.filter(message => this.isOut(message.direction)).length
    },
    selectedMessages () {
      return this.clientMessages(this.selectedClientId)
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
    clientMessages (clientId) {
      return this.messages.filter(
        message => message.routingKey.startsWith(clientId) && !this.isOut(message.direction)
      ).concat(
        this.config.showSentMessages
          ? this.messages.filter(
            message => message.body.senderID === clientId && this.isOut(message.direction)
          )
          : []
      )
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
      const name = this.clientInfos(this.header.clientId).name
      const messageId = uuidv4()
      const targetId = this.clientInfos(this.header.routingKey).id
      const sentAt = moment().format()
      message.distributionID = `${name}_${messageId}`
      message.senderID = this.header.clientId
      message.dateTimeSent = sentAt
      message.descriptor.explicitAddress.explicitAddressValue = targetId
      message.content.contentObject.jsonContent.embeddedJsonContent.message.messageId = messageId
      message.content.contentObject.jsonContent.embeddedJsonContent.message.sender = { name, uri: `hubsante:${this.header.clientId}}` }
      message.content.contentObject.jsonContent.embeddedJsonContent.message.sentAt = sentAt
      message.content.contentObject.jsonContent.embeddedJsonContent.message.recipients.recipient = [{ name: this.clientInfos(this.header.routingKey).name, uri: `hubsante:${targetId}}` }]
      return message
    },
    buildWrongMessage () {
      const message = JSON.parse(JSON.stringify(WRONG_EDXL_ENVELOPE)) // Deep copy
      message.content.contentObject.jsonContent.embeddedJsonContent.message = this.form
      const name = this.clientInfos(this.header.clientId).name
      const messageId = uuidv4()
      const targetId = this.clientInfos(this.header.routingKey).id
      const sentAt = moment().format()
      message.distributionID = `${name}_${messageId}`
      message.senderID = this.header.clientId
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
        { key: this.header.clientId, msg: data },
        { timeout: 1000 }
      ).then(() => {
        this.messages.unshift({
          direction: DIRECTIONS.OUT,
          routingKey: this.header.routingKey,
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
