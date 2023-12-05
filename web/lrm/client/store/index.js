const { SET_CURRENT_USER, TOGGLE_ADVANCED, SET_SHOW_SENT_MESSAGES, ADD_MESSAGE, SET_AUTO_ACK, SET_MESSAGE_JUST_SENT, RESET_MESSAGES } = require('./constants')

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
  messageJustSent: false
})

export const getters = {
  isAuthenticated (state) {
    return !!state.auth.user.clientId
  },

  user (state) {
    return state.auth.user
  },

  isAdvanced (state) {
    return state.auth.user.advanced
  },

  showSentMessages (state) {
    return state.auth.user.showSentMessages
  },

  autoAck (state) {
    return state.auth.user.autoAck
  },

  messages (state) {
    return state.messages
  },

  messageJustSent (state) {
    return state.messageJustSent
  }
}

export const actions = {
  logInUser ({ state, commit }, userData) {
    // use state.auth.user to get default values
    commit(SET_CURRENT_USER, { ...state.auth.user, ...userData })
    return userData
  },

  toggleAdvanced ({ commit, getters }) {
    commit(TOGGLE_ADVANCED)
    return getters.isAdvanced
  },

  setShowSentMessages ({ commit }, showSentMessages) {
    commit(SET_SHOW_SENT_MESSAGES, showSentMessages)
    return showSentMessages
  },

  setAutoAck ({ commit }, autoAck) {
    commit(SET_AUTO_ACK, autoAck)
    return autoAck
  },

  addMessage ({ commit }, message) {
    commit(ADD_MESSAGE, message)
    // If sending message worked well
    if (message.direction === '→') { // isOUt() check
      commit(SET_MESSAGE_JUST_SENT, true)
      setTimeout(() => {
        commit(SET_MESSAGE_JUST_SENT, false)
      }, 1000)
    }
  },

  resetMessages ({ commit }) {
    commit(RESET_MESSAGES)
  }
}

export const mutations = {
  [SET_CURRENT_USER] (state, user) {
    state.auth.user = user
  },

  [TOGGLE_ADVANCED] (state) {
    // Not picked up by Vue reactivity (getter not updated): state.auth.user.advanced = !state.auth.user.advanced
    state.auth.user = {
      ...state.auth.user,
      advanced: !state.auth.user.advanced
    }
  },

  [SET_SHOW_SENT_MESSAGES] (state, showSentMessages) {
    state.auth.user = {
      ...state.auth.user,
      showSentMessages
    }
  },

  [SET_AUTO_ACK] (state, autoAck) {
    state.auth.user = {
      ...state.auth.user,
      autoAck
    }
  },

  [ADD_MESSAGE] (state, message) {
    state.messages.unshift(message)
  },

  [SET_MESSAGE_JUST_SENT] (state, messageJustSent) {
    state.messageJustSent = messageJustSent
  },

  [RESET_MESSAGES] (state) {
    state.messages = []
  }
}
