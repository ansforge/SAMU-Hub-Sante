<template>
  <v-row justify="center">
    <v-col cols="12" sm="7">
      <v-card style="height: 86vh; overflow-y: auto;">
        <v-card-title class="headline pb-">
          Formulaire
          <v-spacer />
          <v-btn color="primary" @click="submit">
            <v-icon left>
              mdi-send
            </v-icon>
            Envoyer
          </v-btn>
        </v-card-title>
        <v-card-text>
          <v-tabs
            v-model="messageTypeTabIndex"
            align-with-title
          >
            <v-tabs-slider color="primary" />
            <v-tab
              v-for="[name, {label}] in Object.entries(messageTypes)"
              :key="name"
            >
              {{ label }}
            </v-tab>
          </v-tabs>
          <v-tabs-items v-model="messageTypeTabIndex">
            <v-tab-item
              v-for="[name, messageTypeDetails] in Object.entries(messageTypes)"
              :key="name"
            >
              <SchemaForm v-bind="messageTypeDetails" ref="schemaForms" :name="name" @sent="messageSent" />
            </v-tab-item>
          </v-tabs-items>
        </v-card-text>
      </v-card>
    </v-col>
    <v-col cols="12" sm="5">
      <v-card style="height: 86vh; overflow-y: auto;">
        <v-card-title class="headline">
          <span class="mb-4">
            Messages
          </span>
          <v-badge
            v-if="showableMessages.length"
            :content="showableMessages.length"
          />
          <v-spacer />
          <v-switch
            v-model="config.showSentMessages"
            inset
            :label="'Show sent (' + messagesSentCount + ')'"
            class="my-0 py-0"
          />
        </v-card-title>
        <v-btn-toggle
          v-model="selectedMessageType"
          class="ml-4"
          dense
          borderless
          mandatory
        >
          <v-btn v-for="{name, type, icon} in queueTypes" :key="type" :value="type" class="px-4">
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
import { DIRECTIONS } from '@/constants'

export default {
  name: 'Demo',
  data () {
    return {
      messageTypeTabIndex: 0,
      messageTypes: {
        createCase: {
          label: 'Partage de dossier',
          schemaName: 'schema.json',
          schema: null,
          examples: [{
            file: 'ExempleDubois.json',
            icon: 'mdi-home-thermometer',
            name: 'Alexandre DUBOIS',
            caller: 'Aide à la personne appelle pour un de ses patients',
            context: 'Malaise pendant canicule',
            environment: 'appartement de la victime',
            victims: '1 victime',
            victim: 'Homme, adulte, 83 ans',
            medicalSituation: 'victime amorphe allongée sur son lit, répond peu, soupçonne une déshydratatio'
          }, {
            file: 'ExempleDurand.json',
            icon: 'mdi-bike-fast',
            name: 'Marin DURAND',
            caller: 'Épouse appelle pour son mari',
            context: 'Collision de 2 vélos',
            environment: 'Voie cyclable à Lyon, gêne de la circulation',
            victims: '2 victimes, 1 nécessitant assistance SAMU.',
            victim: 'Homme, adulte, 43 ans',
            medicalSituation: 'Céphalées, migraines, traumatismes sérieux, plaies intermédiaire'
          }]
        },
        operationRequest: {
          label: 'Demande de concours',
          schemaName: 'schema.json',
          schema: null,
          examples: []
        }
      },
      selectedMessageType: 'message',
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
      queueTypes: [{
        name: 'Message',
        type: 'message',
        icon: 'mdi-message'
      }, {
        name: 'Ack',
        type: 'ack',
        icon: 'mdi-check'
      }, {
        name: 'Info',
        type: 'info',
        icon: 'mdi-information'
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
    clientMessages () {
      return this.messages.filter(
        message => (
          (this.isOut(message.direction) && message.body.senderID === this.user.clientId) ||
            (!this.isOut(message.direction) && message.routingKey.startsWith(this.user.clientId))
        )
      )
    },
    showableMessages () {
      return this.config.showSentMessages ? this.clientMessages : this.clientMessages.filter(message => !this.isOut(message.direction))
    },
    selectedMessages () {
      return this.showableMessages.filter(message => this.getMessageType(message) === this.selectedMessageType)
    },
    messagesSentCount () {
      return this.clientMessages.filter(message => this.isOut(message.direction)).length
    }
  },
  mounted () {
    // To automatically generate the UI and input fields based on the JSON Schema
    Promise.all(Object.entries(this.messageTypes).map(([name, { schemaName }]) => {
      return fetch(schemaName).then(response => response.json()).then(schema => ({ name, schema }))
    })).then((schemas) => {
      schemas.forEach(({ name, schema }) => {
        this.messageTypes[name].schema = schema
      })
    })

    // Start listening to server messages
    const socket = new WebSocket(process.env.wssUrl)
    socket.addEventListener('open', () => {
      console.log('WebSocket demo connection established')
    })
    socket.addEventListener('message', (event) => {
      this.messages.unshift({
        ...JSON.parse(event.data),
        direction: DIRECTIONS.IN,
        receivedTime: this.time()
      })
    })
    socket.addEventListener('close', () => {
      console.log('WebSocket connection closed')
    })
  },
  methods: {
    time () {
      const d = new Date()
      return d.toLocaleTimeString().replace(':', 'h') + '.' + d.getMilliseconds()
    },
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
    typeMessages (type) {
      return this.showableMessages.filter(
        message => this.getMessageType(message) === type
      )
    },
    messageSent (message) {
      this.messages.unshift(message)
    },
    submit () {
      // Submits current SchemaForm
      this.$refs.schemaForms[this.messageTypeTabIndex].submit()
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
