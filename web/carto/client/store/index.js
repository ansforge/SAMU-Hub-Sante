import { defineStore } from 'pinia';

export const useMainStore = defineStore('main', {
  state: () => ({
    socket: null,
    isWebsocketConnected: false,
    _positions: [],
  }),

  getters: {
    positions(state) {
      return state._positions;
    },
  },

  actions: {
    addPosition(position) {
      const index = this._positions.findIndex(
        (p) => p.resourceId === position.resourceId
      );
      if (index !== -1) {
        this._positions[index] = position;
      } else {
        this._positions.push(position);
      }
    },

    resetPositions() {
      this._positions = [];
    },
  },
});
