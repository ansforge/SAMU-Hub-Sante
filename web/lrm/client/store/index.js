const { SET_CURRENT_USER, TOGGLE_ADVANCED } = require('./constants')

export const state = () => ({
  auth: {
    user: {
      clientId: null,
      targetId: null,
      tester: false,
      advanced: false
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
  }
}

export const actions = {
  logInUser ({ commit }, userData) {
    userData.advanced = userData.advanced || false
    commit(SET_CURRENT_USER, userData)
    return userData
  },

  toggleAdvanced ({ commit, getters }) {
    commit(TOGGLE_ADVANCED)
    return getters.isAdvanced
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
  }
}
