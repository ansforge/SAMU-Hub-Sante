import { Channel, Connection, Message } from 'amqplib/callback_api';
import { WebSocketServer, OPEN } from 'ws';

import { logger } from '../logger';
import { RabbitMQConnector } from '../rabbit/utils';
import { Config } from '../config';

const NOT_FOUND_QUEUE_ERROR_MESSAGE_PATTERN = 'NOT_FOUND - no queue';
const MAX_RECONNEXION_ATTEMPT = 3;
const RECONNEXION_ATTEMPT_DELAI = 5000;

export class MessagingService {
  private readonly vhost: string;
  private readonly rabbitMQConnector: RabbitMQConnector;
  private readonly wss: WebSocketServer;
  private readonly config: Config;
  private connection: Connection | undefined;
  private reconnectionAttemptCount: number;

  constructor(vhost: string, config: Config, rabbitMQConnector: RabbitMQConnector, wss: WebSocketServer) {
    this.config = config;
    this.vhost = vhost;
    this.rabbitMQConnector = rabbitMQConnector;
    this.wss = wss;
    this.reconnectionAttemptCount = 0;
  }

  handleConnectionError = (err: unknown) => {
    logger.error(`Connection error for vhost '${this.vhost}': ${err}`);
    // Crash the app
    process.exit();
  };

  handleChannelError = (err: any) => {
    if (this.isMissingQueueError(err)) {
      logger.error(`Missing queue for vhost '${this.vhost}': ${err}`);
      throw err;
    } else {
      logger.error(`Channel error for vhost '${this.vhost}': ${err}`);
      if (this.reconnectionAttemptCount < MAX_RECONNEXION_ATTEMPT) {
        this.reconnect();
      } else {
        throw err;
      }
    }
  };

  reconnect = () => {
    this.reconnectionAttemptCount++;
    logger.info(`trying to reconnect (attempt n°${this.reconnectionAttemptCount})`);
    setTimeout(() => {
      if (this.connection !== undefined) {
        this.connection.createChannel((_, channel) => {
          this.startClientsConsumers(channel);
        });
      }
    }, RECONNEXION_ATTEMPT_DELAI);
  };

  isMissingQueueError = (err: any) => {
    return err.code === 404 && err.message?.includes(NOT_FOUND_QUEUE_ERROR_MESSAGE_PATTERN);
  };

  connectToVhost() {
    this.rabbitMQConnector.connect(this.vhost, (connection: Connection, channel: Channel) => {
      this.connection = connection;
      this.connection.on('error', this.handleConnectionError);
      this.startClientsConsumers(channel);
    });
  }

  startClientsConsumers = (channel: Channel) => {
    channel.on('error', this.handleChannelError);
    this.config.getVhostClientMap()[this.vhost].forEach((clientId) => {
      const service = new ClientListenerService(this.vhost, clientId, this.wss, channel);
      service.listenClientQueues();
    });
  };

  handleCloseConnection = () => {
    if (this.connection) {
      this.rabbitMQConnector.close(this.connection);
      logger.info(`RabbitMQ connection ${this.vhost} shut down`);
    }
  };
}

class ClientListenerService {
  private readonly vhost: string;
  private readonly clientId: string;
  private readonly wss: WebSocketServer;
  private readonly channel: Channel;

  constructor(vhost: string, clientId: string, wss: WebSocketServer, channel: Channel) {
    this.vhost = vhost;
    this.clientId = clientId;
    this.wss = wss;
    this.channel = channel;
  }

  listenClientQueues = () => {
    for (const type of ['message', 'ack', 'info']) {
      const queue = `${this.clientId}.${type}`;
      try {
        this.channel.consume(queue, this.handleConsumeMessage(queue), {
          noAck: true, // Ref.: https://amqp-node.github.io/amqplib/channel_api.html#channelconsume
        });
        logger.info(` [*] Waiting for ${this.clientId} messages in ${queue} (${this.vhost}). To exit press CTRL+C`);
      } catch (err) {
        logger.error(`Error while consuming from queue '${queue}' in vhost '${this.vhost}': ${err}`);
      }
    }
  };

  handleConsumeMessage = (queue: string) => {
    return (msg: Message | null) => {
      // TODO: handle msg content properly
      // eslint-disable-next-line @typescript-eslint/ban-ts-comment
      // @ts-expect-error
      const body = JSON.parse(msg.content);
      logger.info(` [x] Received for ${this.clientId} (${this.vhost}): ${body.distributionID}`);
      logger.debug(
        // TODO: handle msg content properly
        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
        // @ts-expect-error
        ` [x] Received for ${this.clientId} (${this.vhost}): ${body.distributionID} of content ${msg.content}`,
      );
      const d = new Date();
      const data = {
        vhost: this.vhost,
        direction: '←',
        routingKey: queue,
        // Ref.: https://stackoverflow.com/a/9849524
        time: `${d.toLocaleTimeString('fr', { timeZone: 'Europe/Paris' }).replace(':', 'h')}.${String(new Date().getMilliseconds()).padStart(3, '0')}`,
        body,
      };
      // Send the message to all connected WebSocket clients
      let clientCounts = 0;
      this.wss.clients.forEach((client) => {
        if (client.readyState === OPEN) {
          client.send(JSON.stringify(data));
          clientCounts += 1;
        }
      });
      logger.info(`Sent to ${clientCounts} clients: ${data.body.distributionID}`);
      logger.debug(`Sent to ${clientCounts} clients: ${data} of content ${data}`);
    };
  };
}
