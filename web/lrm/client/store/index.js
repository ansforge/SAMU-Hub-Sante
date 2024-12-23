import { useRuntimeConfig } from 'nuxt/app'
import { defineStore } from 'pinia'

function isEnvProd () {
  return process.env.NODE_ENV === 'production'
}

export const useMainStore = defineStore('main', {
  state: () => ({
    vhostMap: Object.keys(useRuntimeConfig().public.vhostMap).map(vhost => ({ vhost, modelVersion: useRuntimeConfig().public.vhostMap[vhost] })),
    selectedVhost: Object.keys(useRuntimeConfig().public.vhostMap).map(vhost => ({ vhost, modelVersion: useRuntimeConfig().public.vhostMap[vhost] }))[0],
    socket: null,
    isWebsocketConnected: false,
    currentMessage: null,
    currentUseCase: null,
    selectedSchema: 'RS-EDA',
    _auth: {
      user: {
        clientId: null,
        targetId: null,
        tester: false,
        advanced: !isEnvProd(),
        showSentMessages: !isEnvProd(),
        autoAck: false
      }
    },
    _messages: [/* {
        direction: DIRECTIONS.IN,
        routingKey: '',
        time: this.timeDisplayFormat(),
        receivedTime: this.timeDisplayFormat(),
        body: { body: 'Page loaded successfully!' }
      } */],
    _messageJustSent: false,
    // ToDo: when message are uploaded, add them in store
    // ToDo: when message is loaded, add them in store to not load them again later
    // Message types are loaded from the github repository
    _messageTypes: []
  }),

  getters: {
    isAuthenticated (state) {
      return !!state._auth.user.clientId
    },

    user (state) {
      return state._auth.user
    },

    demoHeadTitle (state) {
      return 'Démo [' + state._auth.user.clientId?.split('.').splice(2).join('.') + '] - Hub Santé'
    },

    testHeadTitle (state) {
      return 'Test [' + state._auth.user.clientId?.split('.').splice(2).join('.') + '] - Hub Santé'
    },

    isAdvanced (state) {
      return state._auth.user.advanced
    },

    showSentMessages (state) {
      return state._auth.user.showSentMessages
    },

    autoAck (state) {
      return state._auth.user.autoAck
    },

    messages (state) {
      return state._messages
    },

    messageJustSent (state) {
      return state._messageJustSent
    },

    messageTypes (state) {
      return state._messageTypes
    }
  },

  actions: {
    logInUser (userData) {
      // use state.auth.user to get default values
      this._auth.user = userData
      return userData
    },

    toggleAdvanced () {
      this._auth.user = {
        ...this._auth.user,
        advanced: !this._auth.user.advanced
      }
      return this.isAdvanced
    },

    setShowSentMessages (showSentMessages) {
      this._auth.user = {
        ...this._auth.user,
        showSentMessages
      }
      return showSentMessages
    },

    setAutoAck (autoAck) {
      this._auth.user = {
        ...this._auth.user,
        autoAck
      }
      return autoAck
    },

    addMessage (message) {
      this._messages.unshift(message)
      // If sending message worked well
      if (message.direction === '→') { // isOUt() check
        this._messageJustSent = true
        setTimeout(() => {
          this._messageJustSent = false
        }, 1000)
      }
    },

    resetMessages () {
      this._messages = []
    },

    loadSchemas (source) {
      source = source || 'schemas/json-schema/'
      return Promise.all(this.messageTypes.map(async ({ schemaName }, index) => {
        const response = await $fetch(source + schemaName)
        const schema = await JSON.parse(response)
        return ({ index, schema })
      })).then((schemas) => {
        const updatedMessageTypes = []
        schemas.forEach(({ index, schema }) => {
          // TODO: Rethink the whole layout thing
          const objectProps = []
          const simpleProps = []

          // Populate objectProps and simpleProps arrays based on schema properties
          for (const property in schema.properties) {
            if (Object.keys(schema.properties[property]).includes('$ref')) {
              objectProps.push(property)
              // schema.properties[property].layout = 'tabs'
            } else {
              simpleProps.push(property)
            }
          }

          // Set the layout for the schema
          schema.layout = []
          if (simpleProps.length) {
            schema.layout.push({
              children: [...simpleProps]
            })
          }
          if (objectProps.length) {
            schema.layout.push({
              comp: 'tabs',
              children: [...objectProps]
            })
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
            ...this.messageTypes[index],
            schema
          }
        })

        // Reassign the entire array to trigger reactivity
        this._messageTypes = updatedMessageTypes
      })
    },
    loadMessageTypes (source) {
      return fetch(source)
        .then(response => response.json())
        .then((messageTypes) => {
          this._messageTypes = messageTypes
        })
    }
  }
})
