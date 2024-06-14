import Vue from 'vue'
import {
  SET_CURRENT_USER,
  TOGGLE_ADVANCED,
  SET_SHOW_SENT_MESSAGES,
  ADD_MESSAGE,
  SET_AUTO_ACK,
  SET_MESSAGE_JUST_SENT,
  RESET_MESSAGES,
  SET_MESSAGE_TYPE_SCHEMA,
  SET_MESSAGE_TYPES
} from '~/store/constants'

// export const strict = false

export const state = () => ({
  auth: {
    user: {
      clientId: null,
      targetId: null,
      tester: false,
      advanced: process.env.NODE_ENV !== 'production',
      showSentMessages: process.env.NODE_ENV !== 'production',
      autoAck: false
    }
  },
  messages: [/* {
        direction: DIRECTIONS.IN,
        routingKey: '',
        time: this.timeDisplayFormat(),
        receivedTime: this.timeDisplayFormat(),
        body: { body: 'Page loaded successfully!' }
      } */],
  messageJustSent: false,
  // ToDo: when message are uploaded, add them in store
  // ToDo: when message is loaded, add them in store to not load them again later
  // Message types are loaded from the github repository
  messageTypes: []
})

export const getters = {
  isAuthenticated(state) {
    return !!state.auth.user.clientId
  },

  user(state) {
    return state.auth.user
  },

  isAdvanced(state) {
    return state.auth.user.advanced
  },

  showSentMessages(state) {
    return state.auth.user.showSentMessages
  },

  autoAck(state) {
    return state.auth.user.autoAck
  },

  messages(state) {
    return state.messages
  },

  messageJustSent(state) {
    return state.messageJustSent
  },

  messageTypes(state) {
    return state.messageTypes
  }
}

export const actions = {
  logInUser({state, commit}, userData) {
    // use state.auth.user to get default values
    commit(SET_CURRENT_USER, {...state.auth.user, ...userData})
    return userData
  },

  toggleAdvanced({commit, getters}) {
    commit(TOGGLE_ADVANCED)
    return getters.isAdvanced
  },

  setShowSentMessages({commit}, showSentMessages) {
    commit(SET_SHOW_SENT_MESSAGES, showSentMessages)
    return showSentMessages
  },

  setAutoAck({commit}, autoAck) {
    commit(SET_AUTO_ACK, autoAck)
    return autoAck
  },

  addMessage({commit}, message) {
    commit(ADD_MESSAGE, message)
    // If sending message worked well
    if (message.direction === '→') { // isOUt() check
      commit(SET_MESSAGE_JUST_SENT, true)
      setTimeout(() => {
        commit(SET_MESSAGE_JUST_SENT, false)
      }, 1000)
    }
  },

  resetMessages({commit}) {
    commit(RESET_MESSAGES)
  },

  loadSchemas({state, commit}, source) {
    source = source || 'schemas/json-schema/'
    Promise.all(state.messageTypes.map(async ({schemaName}, index) => {
      console.log('Loading schema from: ' + source + schemaName)
      const response = await fetch(source + schemaName)
      const schema = await response.json()
      return ({index, schema})
    })).then((schemas) => {
      schemas.forEach(({index, schema}) => {
        commit(SET_MESSAGE_TYPE_SCHEMA, {index, schema})
      })
    })
  },

  loadMessageTypes ({ state, commit }, source) {
    source = source || 'schemas/messageTypes.json'
    fetch(source)
      .then(response => response.json())
      .then((messageTypes) => {
        commit('SET_MESSAGE_TYPES', messageTypes)
      })
  }
}

export const mutations = {
  [SET_CURRENT_USER](state, user) {
    state.auth.user = user
  },

  [TOGGLE_ADVANCED](state) {
    // Not picked up by Vue reactivity (getter not updated): state.auth.user.advanced = !state.auth.user.advanced
    state.auth.user = {
      ...state.auth.user,
      advanced: !state.auth.user.advanced
    }
  },

  [SET_SHOW_SENT_MESSAGES](state, showSentMessages) {
    state.auth.user = {
      ...state.auth.user,
      showSentMessages
    }
  },

  [SET_AUTO_ACK](state, autoAck) {
    state.auth.user = {
      ...state.auth.user,
      autoAck
    }
  },

  [ADD_MESSAGE](state, message) {
    state.messages.unshift(message)
  },

  [SET_MESSAGE_JUST_SENT](state, messageJustSent) {
    state.messageJustSent = messageJustSent
  },

  [RESET_MESSAGES](state) {
    state.messages = []
  },

  [SET_MESSAGE_TYPE_SCHEMA](state, {index, schema}) {
    Vue.set(state.messageTypes, index, {
      ...this.state.messageTypes[index],
      schema
    })
  },

  [SET_MESSAGE_TYPES](state, messageTypes) {
    state.messageTypes = messageTypes
  }
}
