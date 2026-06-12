import { useRuntimeConfig } from 'nuxt/app';
import { defineStore } from 'pinia';
import { isEnvProd } from '~/composables/envUtils';
import { useAuthStore } from './auth';
import { ref } from 'vue';

export const useMainStore = defineStore('main', {
  state: () => ({
    vhostMap: Object.keys(useRuntimeConfig().public.vhostMap).map((vhost) => ({
      vhost,
      modelVersion:
        useRuntimeConfig().public.vhostMap[vhost]?.model_lib_version ||
        useRuntimeConfig().public.vhostMap[vhost],
      supported_messages:
        useRuntimeConfig().public.vhostMap[vhost]?.supported_messages || [],
    })),
    selectedVhost: Object.keys(useRuntimeConfig().public.vhostMap).map(
      (vhost) => ({
        vhost,
        modelVersion:
          useRuntimeConfig().public.vhostMap[vhost]?.model_lib_version ||
          useRuntimeConfig().public.vhostMap[vhost],
        supported_messages:
          useRuntimeConfig().public.vhostMap[vhost]?.supported_messages || [],
      })
    )[0],
    socket: null,
    isWebsocketConnected: false,
    currentMessage: null,
    currentMessageSenderCaseId: ref(null),
    currentMessageFilePath: null,
    currentUseCase: null,
    selectedSchema: 'RS-EDA',
    alerts: [],
    _auth: {
      user: {
        clientId: null,
        targetId: null,
        tester: false,
        advanced: !isEnvProd(),
        showSentMessages: !isEnvProd(),
        autoAck: false,
      },
    },
    _messages: [
      /* {
        direction: DIRECTIONS.IN,
        routingKey: '',
        time: this.timeDisplayFormat(),
        receivedTime: this.timeDisplayFormat(),
        body: { body: 'Page loaded successfully!' }
      } */
    ],
    _messageJustSent: false,
    // ToDo: when message are uploaded, add them in store
    // ToDo: when message is loaded, add them in store to not load them again later
    // Message types are loaded from the github repository
    _messageTypes: [],
  }),

  getters: {
    demoHeadTitle() {
      const authStore = useAuthStore();

      return (
        'Démo [' +
        authStore.user.clientId?.split('.').splice(2).join('.') +
        '] - Hub Santé'
      );
    },

    testHeadTitle() {
      const authStore = useAuthStore();
      return (
        'Test [' +
        authStore.user.clientId?.split('.').splice(2).join('.') +
        '] - Hub Santé'
      );
    },

    isAdvanced() {
      const authStore = useAuthStore();
      return authStore.user.advanced;
    },

    showSentMessages() {
      const authStore = useAuthStore();
      return authStore.user.showSentMessages;
    },

    autoAck() {
      const authStore = useAuthStore();
      return authStore.user.autoAck;
    },

    messages(state) {
      return state._messages;
    },

    clearMessages() {
      return () => {
        this._messages = [];
      };
    },

    messageJustSent(state) {
      return state._messageJustSent;
    },

    messageTypes(state) {
      const allowed = state.selectedVhost?.supported_messages;
      if (!allowed || allowed.length === 0) return state._messageTypes;
      return state._messageTypes.filter((mt) => allowed.includes(mt.label));
    },
    currentMessageLoaded(state) {
      return !!state.currentMessage?.senderCaseId;
    },
  },

  actions: {
    logInUser(userData) {
      // use state.auth.user to get default values
      const authStore = useAuthStore();
      authStore.user = userData;
      return userData;
    },

    toggleAdvanced() {
      const authStore = useAuthStore();
      authStore.setUser({
        ...authStore.user,
        advanced: !authStore.user.advanced,
      });
      return this.isAdvanced;
    },

    setShowSentMessages(showSentMessages) {
      const authStore = useAuthStore();
      authStore.setUser({
        ...authStore.user,
        showSentMessages,
      });
      return showSentMessages;
    },

    setAutoAck(autoAck) {
      const authStore = useAuthStore();
      authStore.setUser({
        ...authStore.user,
        autoAck,
      });
      return autoAck;
    },

    addMessage(message) {
      this._messages.unshift(message);
      // If sending message worked well
      if (message.direction === '→') {
        // isOUt() check
        this._messageJustSent = true;
        setTimeout(() => {
          this._messageJustSent = false;
        }, 1000);
      }
    },

    addAlertWithTimeout(element, timeout) {
      this.alerts.push(element);
      setTimeout(() => {
        const index = this.alerts.indexOf(element);
        if (index > -1) {
          this.alerts.splice(index, 1);
        }
      }, timeout);
    },

    resetMessages() {
      this._messages = [];
    },

    loadSchemas(source) {
      source = source || 'schemas/json-schema/';
      return Promise.all(
        this._messageTypes.map(async ({ schemaName }, index) => {
          // eslint-disable-next-line no-undef
          const response = await $fetch(source + schemaName);
          const schema = await JSON.parse(response);
          return { index, schema };
        })
      ).then((schemas) => {
        const updatedMessageTypes = [];
        schemas.forEach(({ index, schema }) => {
          // TODO: Rethink the whole layout thing
          const objectProps = [];
          const simpleProps = [];

          // Populate objectProps and simpleProps arrays based on schema properties
          for (const property in schema.properties) {
            if (Object.keys(schema.properties[property]).includes('$ref')) {
              objectProps.push(property);
              // schema.properties[property].layout = 'tabs'
            } else {
              simpleProps.push(property);
            }
          }

          // Set the layout for the schema
          schema.layout = [];
          if (simpleProps.length) {
            schema.layout.push({
              children: [...simpleProps],
            });
          }
          if (objectProps.length) {
            schema.layout.push({
              comp: 'tabs',
              children: [...objectProps],
            });
          }

          // The following attempt doesn't work because if we just set 'layout' to the value of 'x-display' we're defining the type for CHILDREN of the element we're setting it on.
          // We need to set it on the element itself, but for that we have to construct the whole layout array with correctly defined keys and children

          // for (const definition in schema.definitions) {
          //   if (Object.keys(schema.definitions[definition]).includes('x-display')) {
          //     schema.definitions[definition].layout = schema.definitions[definition]['x-display']
          //   }
          // }
          // Add schema to already message type infos
          updatedMessageTypes[index] = {
            ...this._messageTypes[index],
            schema,
          };
        });

        // Reassign the entire array to trigger reactivity
        this._messageTypes = updatedMessageTypes;
      });
    },
    loadMessageTypes(source) {
      // Clear message types before fetching to prevent stale data from previous vhost
      this._messageTypes = [];
      return fetch(source)
        .then((response) => response.json())
        .then((messageTypes) => {
          this._messageTypes = messageTypes;
        });
    },
  },
});
