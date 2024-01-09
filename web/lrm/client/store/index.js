import Vue from 'vue'
import {
  SET_CURRENT_USER,
  TOGGLE_ADVANCED,
  SET_SHOW_SENT_MESSAGES,
  ADD_MESSAGE,
  SET_AUTO_ACK,
  SET_MESSAGE_JUST_SENT,
  RESET_MESSAGES,
  SET_MESSAGE_TYPE_SCHEMA
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
  messageTypes: [{
    label: 'RC-EDA',
    schemaName: 'RC-EDA.schema.json',
    schema: null,
    examples: [{
      file: 'RC-EDA-usecase-Armaury-1.json',
      icon: 'mdi-bike-fast',
      name: 'Alexandre ARMAURY',
      caller: 'Albane Armaury, témoin accident impliquant son mari,  Alexandre Armaury',
      context: 'Collision de 2 vélos',
      environment: 'Voie cyclable à Lyon, gêne de la circulation',
      victims: '2 victimes, 1 nécessitant assistance SAMU',
      victim: 'Homme, adulte, 43 ans',
      medicalSituation: 'Céphalées, migraines, traumatismes sérieux, plaies intermédiaires'
    }, {
      file: '../exemple-invalide.json',
      icon: 'mdi-alert-circle-outline',
      name: 'Champs manquants',
      context: "Pour illustrer les messages d'INFO sur les erreurs de validation"
    }]
  }, {
    label: 'EMSI',
    schemaName: 'EMSI.schema.json',
    schema: null,
    examples: [{
      file: 'EMSI-DC-usecase-Armaury-2.json',
      icon: 'mdi-bike-fast',
      name: 'Alexandre ARMAURY (DC)',
      caller: 'Albane Armaury, témoin accident impliquant son mari,  Alexandre Armaury',
      context: 'Collision de 2 vélos',
      environment: 'Voie cyclable à Lyon, gêne de la circulation',
      victims: '2 victimes, 1 nécessitant assistance SAMU',
      victim: 'Homme, adulte, 43 ans',
      medicalSituation: 'Céphalées, migraines, traumatismes sérieux, plaies intermédiaires'
    }, {
      file: 'EMSI-EO-usecase-Armaury-3.json',
      icon: 'mdi-bike-fast',
      name: 'Alexandre ARMAURY (RDC)',
      caller: 'Albane Armaury, témoin accident impliquant son mari,  Alexandre Armaury',
      context: 'Collision de 2 vélos',
      environment: 'Voie cyclable à Lyon, gêne de la circulation',
      victims: '2 victimes, 1 nécessitant assistance SAMU',
      victim: 'Homme, adulte, 43 ans',
      medicalSituation: 'Céphalées, migraines, traumatismes sérieux, plaies intermédiaires'
    }]
  }, {
    label: 'RS-EDA',
    schemaName: 'RS-EDA.schema.json',
    schema: null,
    examples: [{
      file: 'RS-EDA-usecase-PartageDossier-1.json',
      icon: 'mdi-circular-saw',
      name: 'Didier Morel',
      caller: 'Sébastien Morel, témoin accident impliquant son père, Didier Morel',
      context: 'Accident domestique : blessure grave causée par une scie circulaire électrique',
      environment: 'Domicile, outil scie débranché et sécurisé',
      victims: '1 victime, nécessitant assistance SAMU',
      victim: 'Homme, adulte, 65 ans',
      medicalSituation: 'Plaie traumatique profonde, perte de conscience, hémorragie importante'
    }]
  } /* ,
        info: {
          label: 'RS-INFO',
          schemaName: 'RS-INFO.schema.json',
          schema: null,
          examples: []
        } */
  ]
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
  },

  messageTypes (state) {
    return state.messageTypes
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
  },

  loadSchemas ({ state, commit }, source) {
    // ToDo: load schemas from github branch directly so it is up to date?
    source = source || 'schemas/'
    Promise.all(state.messageTypes.map(async ({ schemaName }, index) => {
      console.log('Loading schema from: ' + source + schemaName)
      const response = await fetch(source + schemaName)
      const schema = await response.json()
      return ({ index, schema })
    })).then((schemas) => {
      schemas.forEach(({ index, schema }) => {
        commit(SET_MESSAGE_TYPE_SCHEMA, { index, schema })
      })
    })
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
  },

  [SET_MESSAGE_TYPE_SCHEMA] (state, { index, schema }) {
    Vue.set(state.messageTypes, index, {
      ...this.state.messageTypes[index],
      schema
    })
  }
}
