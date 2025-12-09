import { Channel, Connection, Message } from 'amqplib/callback_api';
import { WebSocketServer, OPEN } from 'ws';
import { Logger } from 'winston';
import { Histogram, exponentialBuckets } from 'prom-client';

import { getMessageLogsMetadata, logger } from '../logger';
import { RabbitMQConnector } from '../rabbit/utils';
import { Config } from '../config';
import { register } from '../metrics';

const NOT_FOUND_QUEUE_ERROR_MESSAGE_PATTERN = 'NOT_FOUND - no queue';
const MAX_RECONNEXION_ATTEMPT = 3;
const RECONNEXION_ATTEMPT_DELAY = 5000;

const treatmentDurationHistogram = new Histogram({
  name: 'message_treatment_duration',
  help: 'The estimated duration of treatment of a message by the Hub (delta between the dateTimeSent field and the date it has been consumed by the LRM server',
  buckets: exponentialBuckets(0.1, 2, 10),
  labelNames: ["vhost"]
});
register.registerMetric(treatmentDurationHistogram);

export class MessagingService {
  private readonly vhost: string;
  private readonly rabbitMQConnector: RabbitMQConnector;
  private readonly wss: WebSocketServer;
  private readonly config: Config;
  private readonly logger: Logger;
  private connection: Connection | undefined;
  private reconnectionAttemptCount: number;
  private lastReconnectionAttemptTime: number;

  constructor(vhost: string, config: Config, rabbitMQConnector: RabbitMQConnector, wss: WebSocketServer) {
    this.config = config;
    this.vhost = vhost;
    this.rabbitMQConnector = rabbitMQConnector;
    this.wss = wss;
    this.reconnectionAttemptCount = 0;
    this.lastReconnectionAttemptTime = Date.now();
    this.logger = logger.child({ vhost: this.vhost, component: 'MessagingService' });

    this.reconnect = this.reconnect.bind(this);
    this.handleChannelError = this.handleChannelError.bind(this);
    this.handleConnectionError = this.handleConnectionError.bind(this);
    this.connectToVhost = this.connectToVhost.bind(this);
    this.startClientsConsumers = this.startClientsConsumers.bind(this);
  }

  handleConnectionError(err: unknown) {
    this.logger.error(err);
    throw new Error(`Connection error for vhost '${this.vhost}'`);
  }

  handleChannelError(err: any) {
    if (this.isMissingQueueError(err)) {
      this.logger.error(err);
      throw new Error(`Missing queue for vhost '${this.vhost}'`);
    } else {
      this.logger.error(err);
      if (this.reconnectionAttemptCount < MAX_RECONNEXION_ATTEMPT) {
        // Trick to reset the reconnection attemp count, as we don't have access
        // to a callback when the channel connection is successfull.
        if (Date.now() - this.lastReconnectionAttemptTime > RECONNEXION_ATTEMPT_DELAY * 2) {
          this.reconnectionAttemptCount = 0;
        }
        this.reconnect();
      } else {
        this.logger.error(err);
        throw new Error(`Max reconnection attempts reached for vhost '${this.vhost}'`);
      }
    }
  }

  reconnect() {
    this.reconnectionAttemptCount++;
    this.lastReconnectionAttemptTime = Date.now();
    this.logger.info(`Trying to reconnect to vhost '${this.vhost}' (attempt n°${this.reconnectionAttemptCount})`);
    setTimeout(() => {
      if (this.connection !== undefined) {
        this.connection.createChannel((_, channel) => {
          this.startClientsConsumers(channel);
        });
      }
    }, RECONNEXION_ATTEMPT_DELAY);
  }

  isMissingQueueError(err: any) {
    return err?.code === 404 && err.message?.includes(NOT_FOUND_QUEUE_ERROR_MESSAGE_PATTERN);
  }

  connectToVhost() {
    this.rabbitMQConnector.connect(this.vhost, (connection: Connection, channel: Channel) => {
      this.connection = connection;
      this.connection.on('error', this.handleConnectionError);
      this.startClientsConsumers(channel);
    });
  }

  startClientsConsumers(channel: Channel) {
    channel.on('error', this.handleChannelError);
    this.config.getVhostClientMap()[this.vhost].forEach((clientId) => {
      const service = new ClientListenerService(this.vhost, clientId, this.wss, channel);
      service.listenClientQueues();
    });
  }

  handleCloseConnection() {
    if (this.connection) {
      this.rabbitMQConnector.close(this.connection);
      this.logger.info(`RabbitMQ connection ${this.vhost} shut down`);
    }
  }
}

export class ClientListenerService {
  private readonly vhost: string;
  private readonly clientId: string;
  private readonly wss: WebSocketServer;
  private readonly channel: Channel;
  private readonly logger: Logger;

  constructor(vhost: string, clientId: string, wss: WebSocketServer, channel: Channel) {
    this.vhost = vhost;
    this.clientId = clientId;
    this.wss = wss;
    this.channel = channel;
    this.logger = logger.child({ vhost: this.vhost, clientId: this.clientId, component: 'ClientListenerService' });

    this.handleConsumeMessage = this.handleConsumeMessage.bind(this);
    this.listenClientQueues = this.listenClientQueues.bind(this);
  }

  listenClientQueues() {
    for (const type of ['message', 'ack', 'info']) {
      const queue = `${this.clientId}.${type}`;
      try {
        this.channel.consume(queue, this.handleConsumeMessage(queue), {
          noAck: true, // Ref.: https://amqp-node.github.io/amqplib/channel_api.html#channelconsume
        });
        this.logger.info(` [*] Waiting for ${this.clientId} messages in ${queue} (${this.vhost}).`);
      } catch (err) {
        this.logger.error(`Error while consuming from queue '${queue}' in vhost '${this.vhost}': ${err}`);
      }
    }
  }

  handleConsumeMessage(queue: string) {
    return (msg: Message | null) => {
      // TODO: handle msg content properly
      // eslint-disable-next-line @typescript-eslint/ban-ts-comment
      // @ts-expect-error
      const body = JSON.parse(msg.content);
      const logsMetadata = getMessageLogsMetadata(body);
      this.logger.info(` [x] Received for ${this.clientId} (${this.vhost}): ${body.distributionID}`, logsMetadata);
      this.logger.debug(
        // TODO: handle msg content properly
        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
        // @ts-expect-error
        ` [x] Received for ${this.clientId} (${this.vhost}): ${body.distributionID} of content ${msg.content}`,
        logsMetadata,
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
      const treatmentDuration = this.computeTreamtmentDuration(body);
      if (treatmentDuration !== null) treatmentDurationHistogram.labels(this.vhost).observe(treatmentDuration);
      this.logger.info(`Sent to ${clientCounts} clients: ${data.body.distributionID}`, logsMetadata);
      this.logger.debug(`Sent to ${clientCounts} clients: ${data} of content ${data}`, logsMetadata);
    };
  }

  computeTreamtmentDuration(body: any): number | null {
    const logsMetadata = getMessageLogsMetadata(body);
    if (!body || typeof body.dateTimeSent !== 'string') {
      this.logger.warn(
        'Hub treatment duration computation error: missing or invalid dateTimeSent field in message body',
        logsMetadata,
      );
      return null;
    }
    const sentDate = new Date(body.dateTimeSent);
    if (Number.isNaN(sentDate.getTime())) {
      this.logger.warn(
        `Hub treatment duration computation error: could not parse dateTimeSent: ${body.dateTimeSent}`,
        logsMetadata,
      );
      return null;
    }
    const deltaMs = Date.now() - sentDate.getTime();
    if (deltaMs < 0) {
      this.logger.warn(
        `Hub treatment duration computation error: dateTimeSent is in the future: ${body.dateTimeSent}`,
        logsMetadata,
      );
      return null;
    }
    return deltaMs / 1000;
  }
}
