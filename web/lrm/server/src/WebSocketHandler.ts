import { WebSocket, RawData } from 'ws';

import { getMessageLogsMetadata, logger } from './logger';
import { Config } from './config';
import { RabbitMQConnector } from './rabbit/utils';
import { Logger } from "winston";

enum WS_EVENT {
  MESSAGE = 'message',
  CLOSE = 'close',
}

export class WebSocketHandler {
  private readonly ws: WebSocket;
  private readonly config: Config;
  private readonly rabbitMQConnector: RabbitMQConnector;
  private readonly logger: Logger;

  constructor(ws: WebSocket, config: Config, rabbitMQConnector: RabbitMQConnector) {
    this.ws = ws;
    this.config = config;
    this.rabbitMQConnector = rabbitMQConnector;
    this.logger = logger.child({ component: 'WebSocketHandler' });

    this.sendMessage = this.sendMessage.bind(this);
    this.close = this.close.bind(this);
  }

  listen() {
    this.ws.on(WS_EVENT.MESSAGE, this.sendMessage);
    this.ws.on(WS_EVENT.CLOSE, this.close);
  }

  async sendMessage(body: RawData) {
    // TODO: handle body properly
    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-expect-error
    const { key, msg, vhost } = JSON.parse(body);
    const logsMetadata = {
        vhost,
        ...getMessageLogsMetadata(msg)
    }

    this.logger.info(`Received message from WebSocket client: ${msg.distributionID}`, logsMetadata);
    this.logger.debug(`Message content: ${JSON.stringify(msg)}`, logsMetadata);
    try {
      this.logger.info(` [x] Sending msg ${msg.distributionID} to key ${key} (vhost: ${vhost})`, logsMetadata);
      const { connection, channel } = await this.rabbitMQConnector.connectAsync(vhost);
      channel.publish(this.config.getHubSanteExchange(), key, Buffer.from(JSON.stringify(msg)), {
        // Ref.: https://github.com/amqp-node/amqplib/blob/4791f2dfbe8f3bfbd02bb0907e3c35129ae71c13/lib/api_args.js#L231
        contentType: 'application/json',
        deliveryMode: 2,
        priority: 0,
      });
      this.rabbitMQConnector.close(connection);
      this.logger.info(`Publish call done and connection closed for ${msg.distributionID} (vhost: ${vhost})`, logsMetadata);
    } catch (error) {
      this.logger.error(`Error publishing message to RabbitMQ (vhost: ${vhost}): ${error}`, logsMetadata);
    }
  }

  close() {
    this.logger.info('WebSocket client disconnected');
  }
}
