<template>
  <v-row justify="center">
    <v-col cols="12" sm="7">
      <v-card style="height: 86vh; overflow-y: auto;">
        <v-card-title class="text-h5 pb-">
          Formulaire
          <v-spacer />
          <SendButton class="mt-2" @click="submit" />
        </v-card-title>
        <v-card-text>
          <v-tabs
            v-model="messageTypeTabIndex"
            align-tabs="title"
            slider-color="primary"
          >
            <v-tab
              v-for="[name, {label}] in Object.entries(store.messageTypes)"
              :key="name"
            >
              {{ label }}
            </v-tab>
          </v-tabs>
          <v-window v-model="messageTypeTabIndex" fixed-tabs>
            <v-window-item
              v-for="[name, messageTypeDetails] in Object.entries(store.messageTypes)"
              :key="name"
            >
              <SchemaForm v-bind="messageTypeDetails" ref="schemaForms" :name="name" />
            </v-window-item>
          </v-window>
        </v-card-text>
      </v-card>
    </v-col>
    <v-col cols="12" sm="5">
      <v-card style="height: 86vh; overflow-y: auto;">
        <v-card-title class="text-h5">
          <span class="mb-4">
            {{ showSentMessagesConfig ? 'Messages' : 'Messages reçus' }}
          </span>
          <v-badge
            v-if="showableMessages?.length"
            class="mb-4"
            :content="showableMessages?.length"
          />
          <v-spacer />
          <v-switch
            v-if="store.isAdvanced"
            v-model="store.autoAckConfig"
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
          v-model="store.selectedMessageType"
          class="ml-4"
          density="compact"
          borderless
          mandatory
        >
          <v-btn v-for="{name, type, icon} in queueTypes" :key="type" :value="type" class="px-4">
            <v-icon start>
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
            variant="outlined"
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
              @use-message-to-reply="useMessageToReply"
            />
          </transition-group>
        </v-card-text>
      </v-card>
    </v-col>
  </v-row>
</template>

<script>
import mixinMessage from '~/mixins/mixinMessage'
import { REPOSITORY_URL } from '@/constants'
import { useMainStore } from '~/store'

const config = useRuntimeConfig()

export default {
  name: 'Demo',
  mixins: [mixinMessage],
  data () {
    return {
      store: useMainStore(),
      messageTypeTabIndex: 0,
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
    showSentMessagesConfig: {
      get () {
        return this.store.showSentMessages
      },
      set (value) {
        this.store.setShowSentMessages(value)
      }
    },
    autoAckConfig: {
      get () {
        return this.autoAck
      },
      set (value) {
        this.store.setAutoAck(value)
      }
    },
    clientMessages () {
      return this.store.messages.filter(
        message => (
          (this.isOut(message.direction) && message.body.senderID === this.store.user.clientId) ||
          (!this.isOut(message.direction) && message.routingKey.startsWith(this.store.user.clientId))
        )
      )
    },
    showableMessages () {
      return this.store.showSentMessages ? this.clientMessages : this.clientMessages?.filter(message => !this.isOut(message.direction))
    },
    selectedTypeMessages () {
      return this.showableMessages.filter(message => this.getMessageType(message) === this.store.selectedMessageType)
    },
    selectedTypeCaseMessages () {
      if (this.selectedCaseIds.length === 0) {
        return this.selectedTypeMessages
      }
      return this.selectedTypeMessages.filter(
        message => this.selectedCaseIds.includes(this.getCaseId(message, true))
      )
    },
    messagesSentCount () {
      return this.clientMessages.filter(message => this.isOut(message.direction)).length
    },
    caseIds () {
      return [...new Set(this.selectedTypeMessages.map(m => this.getCaseId(m, true)))]
    }
  },
  mounted () {
    // To automatically generate the UI and input fields based on the JSON Schema
    // We need to wait the acquisition of 'messagesList' before attempting to acquire the schemas
    this.store.loadMessageTypes(REPOSITORY_URL + config.public.modelBranch + '/src/main/resources/sample/examples/messagesList.json').then(
      () => this.store.loadSchemas(REPOSITORY_URL + config.public.modelBranch + '/src/main/resources/json-schema/')
    )
  },
  methods: {
    typeMessages (type) {
      return this.showableMessages.filter(
        message => this.getMessageType(message) === type
      )
    },
    submit () {
      // Submits current SchemaForm
      console.log('submitting:', this.store.messageTypes[this.messageTypeTabIndex].label)
      this.$refs.schemaForms.find(schema => schema.label === this.store.messageTypes[this.messageTypeTabIndex].label).submit()
    },
    useMessageToReply (message) {
      // Use message to fill the form
      this.$refs.schemaForms.find(schema => schema.label === this.store.messageTypes[this.messageTypeTabIndex].label).load(message)
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
