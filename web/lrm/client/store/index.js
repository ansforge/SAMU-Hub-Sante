const { SET_CURRENT_USER, TOGGLE_ADVANCED, SET_SHOW_SENT_MESSAGES } = require('./constants')

export const state = () => ({
  auth: {
    user: {
      clientId: null,
      targetId: null,
      tester: false,
      advanced: process.env.NODE_ENV !== 'production',
      showSentMessages: process.env.NODE_ENV !== 'production'
    }
  }
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
  }
}
