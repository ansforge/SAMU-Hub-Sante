<template>
  <v-row justify="center">
    <v-col cols="12" sm="7">
      <v-card style="height: 86vh; overflow-y: auto">
        <v-card-title class="text-h5 d-flex justify-space-between align-center">
          <span class="mr-5">Formulaire</span>
          <vhost-selector class="mr-5" />
          <send-button @click="submit(store.currentMessage)" />
        </v-card-title>
        <v-card-text>
          <v-tabs
            v-model="messageTypeTabIndex"
            data-cy="message-type-tabs"
            show-arrows
            align-tabs="title"
          >
            <v-tabs color="primary" />
            <v-tab v-for="{ label } in store.messageTypes" :key="label">
              {{ label }}
            </v-tab>
          </v-tabs>
          <v-window
            v-if="store.messageTypes.length"
            v-model="messageTypeTabIndex"
            fixed-tabs
          >
            <schema-form
              v-if="currentMessageType"
              ref="schemaForm"
              :source="source"
              :current-message-type="currentMessageType"
              :message-type-tab-index="messageTypeTabIndex"
            />
          </v-window>
        </v-card-text>
      </v-card>
    </v-col>
    <v-col cols="12" sm="5">
      <v-card style="height: 86vh; overflow-y: auto">
        <v-card-title class="text-h5 d-flex">
          <span class="mb-4">
            {{ showSentMessagesConfig ? 'Messages' : 'Messages reçus' }}
          </span>
          <v-badge
            v-if="showableMessages?.length"
            floating
            offset-y="50%"
            color="primary"
            class="mr-2 ml-2"
            :content="showableMessages?.length"
          />
          <v-spacer />
          <v-switch
            v-if="store.isAdvanced"
            v-model="authStore.user.autoAck"
            inset
            color="primary"
            :label="'Auto ack'"
            class="my-0 py-0 mr-4"
          />
          <v-switch
            v-model="showSentMessagesConfig"
            inset
            color="primary"
            :label="'Show sent (' + messagesSentCount + ')'"
            class="my-0 py-0"
          />
        </v-card-title>
        <v-btn-toggle
          v-model="selectedMessageType"
          class="ml-4"
          density="compact"
          mandatory
        >
          <v-btn
            v-for="{ name, type, icon } in queueTypes"
            :key="type"
            :value="type"
            class="px-4"
          >
            <v-icon start>
              {{ icon }}
            </v-icon>
            {{ name }}
            <v-badge
              v-if="typeMessages(type).length > 0"
              floating
              color="primary"
              class="mr-2 ml-2"
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

<script setup>
import { toRef } from 'vue';
import { toast } from 'vue3-toastify';
import { consola } from 'consola';
import mixinWebsocket from '~/mixins/mixinWebsocket';
import { useMainStore } from '~/store';
import { useAuthStore } from '@/store/auth';

import {
  buildMessage,
  sendMessage,
  isOut,
  getMessageType,
  getCaseId,
} from '~/composables/messageUtils.js';
import { loadSchemas } from '~/composables/schemaUtils';

function submit(form) {
  try {
    const data = buildMessage({
      [useMainStore().currentUseCase]: form,
    });
    sendMessage(data);
  } catch (error) {
    console.error("Erreur lors de l'envoi du message", error);
  }
}

// eslint-disable-next-line no-undef
useHead({
  titleTemplate: toRef(useMainStore(), 'demoHeadTitle'),
});
</script>

<script>
export default {
  name: 'Demo',
  mixins: [mixinWebsocket],
  data() {
    return {
      config: null,
      source: null,
      store: useMainStore(),
      authStore: useAuthStore(),
      messageTypeTabIndex: null,
      selectedMessageType: 'message',
      selectedClientId: null,
      selectedCaseIds: [],
      queueTypes: [
        {
          name: 'Message',
          type: 'message',
          icon: 'mdi-message',
        },
        {
          name: 'Ack',
          type: 'ack',
          icon: 'mdi-check',
        },
        {
          name: 'Info',
          type: 'info',
          icon: 'mdi-information',
        },
      ],
      form: {},
    };
  },
  computed: {
    currentMessageType() {
      return this.store.messageTypes[this.messageTypeTabIndex];
    },
    showSentMessagesConfig: {
      get() {
        return this.store.showSentMessages;
      },
      set(value) {
        this.store.setShowSentMessages(value);
      },
    },
    clientMessages() {
      return this.store.messages.filter(
        (message) =>
          (isOut(message.direction) &&
            message.body.senderID === this.authStore.user.clientId) ||
          (!isOut(message.direction) &&
            message.routingKey.startsWith(this.authStore.user.clientId))
      );
    },
    showableMessages() {
      return this.store.showSentMessages
        ? this.clientMessages
        : this.clientMessages?.filter((message) => !isOut(message.direction));
    },
    selectedTypeMessages() {
      return this.showableMessages.filter(
        (message) => getMessageType(message) === this.selectedMessageType
      );
    },
    selectedVhost() {
      return this.store.selectedVhost;
    },
    selectedTypeCaseMessages() {
      if (this.selectedCaseIds.length === 0) {
        return this.selectedTypeMessages;
      }
      return this.selectedTypeMessages.filter((message) =>
        this.selectedCaseIds.includes(getCaseId(message, true))
      );
    },
    messagesSentCount() {
      return this.clientMessages.filter((message) => isOut(message.direction))
        .length;
    },
    caseIds() {
      return [
        ...new Set(this.selectedTypeMessages.map((m) => getCaseId(m, true))),
      ];
    },
  },
  watch: {
    source() {
      this.updateForm();
    },
    currentMessageType() {
      this.store.selectedSchema =
        this.store.messageTypes[this.messageTypeTabIndex];
      this.store.currentUseCase =
        this.store.messageTypes[this.messageTypeTabIndex].schema.title;
    },
    selectedVhost() {
      this.source = this.store.selectedVhost.modelVersion;
    },
  },
  mounted() {
    this.source = this.store.selectedVhost.modelVersion;
  },
  methods: {
    updateForm() {
      // To automatically generate the UI and input fields based on the JSON Schema
      // We need to wait the acquisition of 'messagesList' before attempting to acquire the schemas
      loadSchemas().then(() => {
        consola.log('messagesList.json and schemas loaded for ' + this.source);
        this.messageTypeTabIndex = 0;
      });
    },
    typeMessages(type) {
      return this.showableMessages.filter(
        (message) => getMessageType(message) === type
      );
    },
    useMessageToReply(message) {
      // Use message to fill the form
      if (message[this.store.selectedSchema.schema.title]) {
        this.store.currentMessage =
          message[this.store.selectedSchema.schema.title];
      } else {
        // TODO: automatically switch to the corresponding schema?
        toast.error('Le message ne correspond pas au schéma sélectionné');
      }
    },
  },
};
</script>
<style>
.message {
  transition: all 0.5s;
}

.message-enter, .message-leave-to
  /* .message-leave-active for <2.1.8 */ {
  opacity: 0;
  transform: scale(0.7) translateY(-500px);
}

.message-enter-to {
  opacity: 1;
  transform: scale(1);
}

.message-leave-active {
  /* position: absolute; */
}

.message-move {
  opacity: 1;
  transition: all 0.5s;
}
</style>
