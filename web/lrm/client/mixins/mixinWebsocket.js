import { consola } from 'consola';
import { DIRECTIONS } from '@/constants';
import mixinUser from '~/mixins/mixinUser';
import { useMainStore } from '~/store';
import {
  buildAck,
  sendMessage,
  getMessageType,
  getReadableMessageType,
  timeDisplayFormat,
} from '~/composables/messageUtils';

export default {
  mixins: [mixinUser],
  data: () => ({
    store: useMainStore(),
  }),
  mounted() {
    if (this.store.socket === null && this.$route.name !== 'json') {
      this.wsConnect();
    }
  },
  beforeRouteLeave(to, from, next) {
    this.wsDisconnect();
    next();
  },
  methods: {
    wsConnect() {
      this.store.socket = new WebSocket(
        'wss://' + this.$config.public.backendLrmServer + '/lrm/api/'
      );
      this.store.socket.onopen = () => {
        this.store.isWebsocketConnected = true;
        consola.log(`WebSocket ${this.$options.name} connection established`);
      };

      this.store.socket.onclose = (e) => {
        this.store.isWebsocketConnected = false;
        // Prevents infinite loop when closing the connection in an expected way
        if (this.disconnect) {
          return;
        }
        consola.log(`WebSocket ${this.$options.name} connection closed`, e);
        // Retry connection
        setTimeout(() => {
          this.wsConnect();
        }, 1000);
      };

      this.store.socket.onerror = (err) => {
        consola.error(
          `WebSocket ${this.$options.name} connection errored`,
          err
        );
        this.store.socket.close();
        this.store.isWebsocketConnected = false;
      };

      // demo.vue is in charge of listening to server messages
      if (this.$options.name === 'Demo' || this.$options.name === 'Testcase') {
        this.store.socket.addEventListener('message', (event) => {
          const message = JSON.parse(event.data);
          this.store.addMessage({
            ...message,
            direction: DIRECTIONS.IN,
            messageType: getReadableMessageType(message.body.distributionKind),
            receivedTime: timeDisplayFormat(),
          });
          if (this.autoAck) {
            // Send back acks automatically to received messages
            if (
              getMessageType(message) !== 'ack' &&
              message.routingKey.startsWith(this.store.user.clientId)
            ) {
              const msg = buildAck(message.body.distributionID);
              sendMessage(msg);
            }
          }
        });
      }
    },
    wsDisconnect() {
      if (this.store.socket) {
        consola.log(
          `Disconnecting: WebSocket ${this.$options.name} connection closed`
        );
        this.store.socket.close();
      }
      this.disconnect = true;
    },
  },
};
