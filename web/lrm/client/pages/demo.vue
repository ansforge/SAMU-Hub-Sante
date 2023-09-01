<template>
  <v-row justify="center">
    <v-col cols="12" sm="7">
      <v-card style="height: 86vh; overflow-y: auto;">
        <v-card-title class="headline pb-">
          Formulaire
          <v-spacer />
          <SendButton class="mt-2" @click="submit" />
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
              <SchemaForm v-bind="messageTypeDetails" ref="schemaForms" :name="name" />
            </v-tab-item>
          </v-tabs-items>
        </v-card-text>
      </v-card>
    </v-col>
    <v-col cols="12" sm="5">
      <v-card style="height: 86vh; overflow-y: auto;">
        <v-card-title class="headline">
          <span class="mb-4">
            {{ showSentMessagesConfig ? 'Messages' : 'Messages reçus' }}
          </span>
          <v-badge
            v-if="showableMessages.length"
            class="mb-4"
            :content="showableMessages.length"
          />
          <v-spacer />
          <v-switch
            v-model="autoAckConfig"
            inset
            :label="'Auto ack'"
            class="my-0 py-0 mr-4"
          />
          <v-switch
            v-model="showSentMessagesConfig"
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
        <v-chip-group
          v-if="selectedMessageType === 'message' && caseIds.length > 1"
          v-model="selectedCaseIds"
          class="ml-4"
          multiple
        >
          <v-chip
            v-for="caseId in caseIds"
            :key="caseId"
            :value="caseId"
            filter
            outlined
          >
            {{ caseId }}
          </v-chip>
        </v-chip-group>
        <v-card-text>
          <transition-group name="message">
            <ReceivedMessage
              v-for="message in selectedTypeCaseMessages"
              v-bind="message"
              :key="message.time"
              class="message mb-4"
              @useMessageToReply="useMessageToReply"
            />
          </transition-group>
        </v-card-text>
      </v-card>
    </v-col>
  </v-row>
</template>

<script>
import { mapGetters } from 'vuex'
import mixinMessage from '~/plugins/mixinMessage'

export default {
  name: 'Demo',
  mixins: [mixinMessage],
  data () {
    return {
      messageTypeTabIndex: 0,
      messageTypes: {
        createCase: {
          label: 'Partage de dossier',
          schemaName: 'schema.json',
          schema: null,
          examples: [{
            file: 'exempleBlanchet.json',
            icon: 'mdi-home-thermometer',
            name: 'Bastien BLANCHET',
            caller: 'Aide à la personne appelle pour un de ses patients',
            context: 'Malaise pendant canicule',
            environment: 'appartement de la victime',
            victims: '1 victime',
            victim: 'Homme, adulte, 82 ans',
            medicalSituation: 'victime amorphe allongée sur son lit, répond peu, soupçonne une déshydratatio'
          }, {
            file: 'exempleArmaury.json',
            icon: 'mdi-bike-fast',
            name: 'Jean ARMAURY',
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
      selectedClientId: null,
      selectedCaseIds: [],
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
      title: `Démo [${this.userInfos.name}]`
    }
  },
  computed: {
    ...mapGetters(['messages']),
    showSentMessagesConfig: {
      get () {
        return this.showSentMessages
      },
      set (value) {
        this.$store.dispatch('setShowSentMessages', value)
      }
    },
    autoAckConfig: {
      get () {
        return this.autoAck
      },
      set (value) {
        this.$store.dispatch('setAutoAck', value)
      }
    },
    clientMessages () {
      return this.messages.filter(
        message => (
          (this.isOut(message.direction) && message.body.senderID === this.user.clientId) ||
            (!this.isOut(message.direction) && message.routingKey.startsWith(this.user.clientId))
        )
      )
    },
    showableMessages () {
      return this.showSentMessages ? this.clientMessages : this.clientMessages.filter(message => !this.isOut(message.direction))
    },
    selectedTypeMessages () {
      return this.showableMessages.filter(message => this.getMessageType(message) === this.selectedMessageType)
    },
    selectedTypeCaseMessages () {
      if (this.selectedCaseIds.length === 0) {
        return this.selectedTypeMessages
      }
      return this.selectedTypeMessages.filter(
        message => this.selectedCaseIds.includes(this.getCaseId(message))
      )
    },
    messagesSentCount () {
      return this.clientMessages.filter(message => this.isOut(message.direction)).length
    },
    caseIds () {
      return [...new Set(this.selectedTypeMessages.map(this.getCaseId))]
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
  },
  methods: {
    typeMessages (type) {
      return this.showableMessages.filter(
        message => this.getMessageType(message) === type
      )
    },
    submit () {
      // Submits current SchemaForm
      this.$refs.schemaForms[this.messageTypeTabIndex].submit()
    },
    useMessageToReply (message) {
      // Use message to fill the form
      this.$refs.schemaForms[this.messageTypeTabIndex].load(message)
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
