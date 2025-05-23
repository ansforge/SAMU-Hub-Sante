import { Server as HttpServer, createServer } from 'http';
import express from 'express';
import cors from 'cors';
import cookieParser from 'cookie-parser';
import bodyParser from 'body-parser';
import { Server as WssServer, OPEN } from 'ws';
import { logger } from './logger';
import { connect, connectAsync, close, HUB_SANTE_EXCHANGE, VHOST_CLIENT_MAP, messageProperties } from './rabbit/utils';
import { ModelesRouter } from './router/modelesRouter';

import { Express } from 'express';
import { Channel, Connection } from 'amqplib/callback_api';

export class ExpressServer {
  private port: number;
  private app: Express;
  private connections: Record<string, Connection>;
  private wss: WssServer | undefined;
  private server: HttpServer | undefined;

  constructor(port: number) {
    this.port = port;
    this.app = express();
    this.connections = {};
    this.setupMiddleware();
  }

  setupMiddleware() {
    this.app.use(cors());
    this.app.use(bodyParser.json({ limit: '14MB' }));
    this.app.use(express.json());
    this.app.use(express.urlencoded({ extended: false }));
    this.app.use(cookieParser());

    this.app.use('/modeles', ModelesRouter);

    // Subscribe to Hub messages and send them to the client through web socket
    logger.info(`VHOST_CLIENT_MAP: ${JSON.stringify(VHOST_CLIENT_MAP)}`);
    // Get list of keys (corresponding to vhosts) from the VHOSTS map
    const vhostsArray = Object.keys(VHOST_CLIENT_MAP);
    for (const vhost of vhostsArray) {
      connect(vhost, async (connection: Connection, channel: Channel) => {
        connection.on('error', (err) => {
          logger.error(`Connection error for vhost '${vhost}': ${err}`);
        });

        this.connections[vhost] = connection;

        // Add error handler to channel
        channel.on('error', (err) => {
          // If it's a NOT-FOUND error for a queue, log it but allow execution to continue
          if (err.code === 404 && err.message && err.message.includes('NOT_FOUND - no queue')) {
            if (err.message.includes('fr.health.test.samuv')) {
              logger.info(`Test SAMU with specific version has no queue (likely to be expected): '${vhost}': ${err}`);
            } else {
              logger.error(`Missing queue for vhost '${vhost}': ${err}`);
            }
          } else {
            logger.error(`Channel error for vhost '${vhost}': ${err}`);
          }
        });

        for (const clientId of VHOST_CLIENT_MAP[vhost]) {
          console.log(`Client ID: ${clientId}`);
          for (const type of ['message', 'ack', 'info']) {
            const queue = `${clientId}.${type}`;
            try {
              channel.consume(
                queue,
                (msg) => {
                  // TODO: handle msg content properly
                  // eslint-disable-next-line @typescript-eslint/ban-ts-comment
                  // @ts-expect-error
                  const body = JSON.parse(msg.content);
                  logger.info(` [x] Received for ${clientId} (${vhost}): ${body.distributionID}`);
                  logger.debug(
                    // TODO: handle msg content properly
                    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
                    // @ts-expect-error
                    ` [x] Received for ${clientId} (${vhost}): ${body.distributionID} of content ${msg.content}`,
                  );
                  const d = new Date();
                  const data = {
                    vhost,
                    direction: '←',
                    routingKey: queue,
                    // Ref.: https://stackoverflow.com/a/9849524
                    time: `${d.toLocaleTimeString('fr', { timeZone: 'Europe/Paris' }).replace(':', 'h')}.${String(new Date().getMilliseconds()).padStart(3, '0')}`,
                    body,
                  };
                  // Send the message to all connected WebSocket clients
                  let clientCounts = 0;
                  // TODO: init wss in constructor and remove condition
                  if (this.wss) {
                    this.wss.clients.forEach((client) => {
                      if (client.readyState === OPEN) {
                        client.send(JSON.stringify(data));
                        clientCounts += 1;
                      }
                    });
                    logger.info(`Sent to ${clientCounts} clients: ${data.body.distributionID}`);
                    logger.debug(`Sent to ${clientCounts} clients: ${data} of content ${data}`);
                  }
                },
                {
                  noAck: true, // Ref.: https://amqp-node.github.io/amqplib/channel_api.html#channelconsume
                },
              );
              logger.info(` [*] Waiting for ${clientId} messages in ${queue} (${vhost}). To exit press CTRL+C`);
            } catch (err) {
              logger.error(`Error while consuming from queue '${queue}' in vhost '${vhost}': ${err}`);
            }
          }
        }
      });
    }
  }

  launch() {
    // TODO: handle error handling middleware typing properly
    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-expect-error
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    this.app.use((err, req, res, next) => {
      // format errors
      res.status(err.status || 500).json({
        message: err.message || err,
        errors: err.errors || '',
      });
    });

    this.server = createServer(this.app).listen(this.port);
    this.wss = new WssServer({ server: this.server });
    // WebSocket server
    this.wss.on('connection', (ws) => {
      logger.info('WebSocket client connected');

      ws.on('message', async (body) => {
        // Publish the message to RabbitMQ
        // TODO: handle body properly
        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
        // @ts-expect-error
        const { key, msg, vhost } = JSON.parse(body);
        logger.info(`Received message from WebSocket client: ${msg.distributionID}`);
        logger.debug(`Received message from WebSocket client: ${msg.distributionID} of content ${body}`);
        logger.info(` [x] Sending msg ${msg.distributionID} to key ${key} (vhost: ${vhost})`);
        try {
          const { connection, channel } = await connectAsync(vhost);
          channel.publish(HUB_SANTE_EXCHANGE, key, Buffer.from(JSON.stringify(msg)), messageProperties);
          close(connection);
          logger.info(`Publish call done and connection closed for ${msg.distributionID} (vhost: ${vhost})`);
        } catch (error) {
          logger.error(`Error publishing message to RabbitMQ (vhost: ${vhost}): ${error}`);
        }
      });

      ws.on('close', () => {
        logger.info('WebSocket client disconnected');
      });
    });
    logger.info(`Listening on port ${this.port}`);
  }

  async close() {
    for (const [vhost, connection] of Object.entries(this.connections)) {
      if (connection !== undefined) {
        close(connection);
        logger.info(`RabbitMQ connection ${vhost} shut down`);
      }
    }
    if (this.server !== undefined) {
      await this.server.close();
      logger.info(`Server on port ${this.port} shut down`);
    }
  }
}
