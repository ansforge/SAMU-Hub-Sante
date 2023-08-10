const { SET_CURRENT_USER } = require('./constants')

export const state = () => ({
  auth: {
    user: {
      clientId: null,
      targetId: null,
      tester: false
    }
  }
})

export const getters = {
  isAuthenticated (state) {
    return !!state.auth.user.clientId
  },

  user (state) {
    return state.auth.user
  }
}

export const actions = {
  logInUser ({ commit }, userData) {
    commit(SET_CURRENT_USER, userData)
    return userData
  }
}

export const mutations = {
  [SET_CURRENT_USER] (state, user) {
    state.auth.user = user
  }
}
