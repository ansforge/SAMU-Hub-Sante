import { ref, onMounted } from 'vue';
import { consola } from 'consola';
import { useMainStore } from '~/store';
import { useRuntimeConfig } from '#app';
import { isEnvProd } from '~/composables/envUtils';

export function useWebSocket(componentName) {
  const store = useMainStore();
  const config = useRuntimeConfig();
  const disconnect = ref(false);

  function wsConnect() {
    store.socket = new WebSocket(getServerUrl() + '/carto/api/');

    store.socket.onopen = () => {
      store.isWebsocketConnected = true;
      consola.log(`WebSocket ${componentName} connection established`);
    };

    store.socket.onclose = (e) => {
      store.isWebsocketConnected = false;
      // Prevents infinite loop when closing the connection in an expected way
      if (disconnect.value) {
        return;
      }
      consola.log(`WebSocket ${componentName} connection closed`, e);
      // Retry connection
      setTimeout(() => {
        wsConnect();
      }, 1000);
    };

    store.socket.onerror = (err) => {
      consola.error(`WebSocket ${componentName} connection errored`, err);
      store.socket.close();
      store.isWebsocketConnected = false;
    };

    // Only Carto component listens to position updates
    if (componentName === 'Carto') {
      listenPositions();
    }
  }

  function wsDisconnect() {
    if (store.socket) {
      consola.log(
        `Disconnecting: WebSocket ${componentName} connection closed`
      );
      store.socket.close();
    }
    disconnect.value = true;
  }

  function listenPositions() {
    store.socket.addEventListener('message', (event) => {
      const message = JSON.parse(event.data);
      const positions =
        message.body.content[0].jsonContent.embeddedJsonContent.message
          .geoPositionsUpdate.position;
      if (positions) {
        positions.forEach((position) => {
          store.addPosition({
            ...position,
          });
        });
      }
    });
  }

  function getServerUrl() {
    return `${isEnvProd() ? 'wss' : 'ws'}://${
      config.public.backendCartoServer
    }`;
  }

  onMounted(() => {
    if (store.socket === null) {
      wsConnect();
    }
  });

  // eslint-disable-next-line no-undef
  onBeforeRouteLeave((to, from, next) => {
    wsDisconnect();
    next();
  });

  return {
    wsConnect,
    wsDisconnect,
    isConnected: store.isWebsocketConnected,
  };
}
