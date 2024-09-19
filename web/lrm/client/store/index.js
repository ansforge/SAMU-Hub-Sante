import { defineStore } from 'pinia'
import { REPOSITORY_URL } from '@/constants'

// export const strict = false
export const useMainStore = defineStore('main', {
  state: () => ({
    currentMessage: null,
    currentUseCase: null,
    selectedSource: 'main',
    selectedSchema: 'RS-EDA',
    _auth: {
      user: {
        clientId: null,
        targetId: null,
        tester: false,
        advanced: process.env.NODE_ENV !== 'production',
        showSentMessages: process.env.NODE_ENV !== 'production',
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
      // ToDo: load schemas from github branch directly so it is up to date?
      source = source || 'schemas/json-schema/'
      return Promise.all(this.messageTypes.map(async ({ schemaName }, index) => {
        console.log('Loading schema from: ' + source + schemaName)
        const response = await $fetch(source + schemaName)
        const schema = await JSON.parse(response)
        return ({ index, schema })
      })).then((schemas) => {
        schemas.forEach(({ index, schema }) => {
          this.messageTypes[index] = {
            ...this.messageTypes[index],
            schema
          }
        })
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
