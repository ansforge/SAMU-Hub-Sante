import { WebSocket, RawData } from 'ws';

import { logger } from './logger';
import { Config } from './config';
import { RabbitMQConnector } from './rabbit/utils';

enum WS_EVENT {
  MESSAGE = 'message',
  CLOSE = 'close',
}

export class WebSocketHandler {
  private readonly ws: WebSocket;
  private readonly config: Config;
  private readonly rabbitMQConnector: RabbitMQConnector;

  constructor(ws: WebSocket, config: Config, rabbitMQConnector: RabbitMQConnector) {
    this.ws = ws;
    this.config = config;
    this.rabbitMQConnector = rabbitMQConnector;
  }

  listen = () => {
    this.ws.on(WS_EVENT.MESSAGE, this.sendMessage);
    this.ws.on(WS_EVENT.CLOSE, this.close);
  }

  sendMessage = async (body: RawData) => {
    // TODO: handle body properly
    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-expect-error
    const { key, msg, vhost } = JSON.parse(body);
    logger.info(`Received message from WebSocket client: ${msg.distributionID}`);
    logger.debug(`Received message from WebSocket client: ${msg.distributionID} of content ${body}`);
    logger.info(` [x] Sending msg ${msg.distributionID} to key ${key} (vhost: ${vhost})`);
    try {
      const { connection, channel } = await this.rabbitMQConnector.connectAsync(vhost);
      channel.publish(this.config.getHubSanteExchange(), key, Buffer.from(JSON.stringify(msg)), {
        // Ref.: https://github.com/amqp-node/amqplib/blob/4791f2dfbe8f3bfbd02bb0907e3c35129ae71c13/lib/api_args.js#L231
        contentType: 'application/json',
        deliveryMode: 2,
        priority: 0,
      });
      this.rabbitMQConnector.close(connection);
      logger.info(`Publish call done and connection closed for ${msg.distributionID} (vhost: ${vhost})`);
    } catch (error) {
      logger.error(`Error publishing message to RabbitMQ (vhost: ${vhost}): ${error}`);
    }
  }

  close = () => {
    logger.info('WebSocket client disconnected');
  }
}
