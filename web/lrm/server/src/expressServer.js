// const { Middleware } = require('swagger-express-middleware');
const http = require('http');
const path = require('path');
const express = require('express');
const cors = require('cors');
const cookieParser = require('cookie-parser');
const bodyParser = require('body-parser');
const WebSocket = require('ws');
const logger = require('./logger').logger;
const {
  connect, connectAsync, close,
  HUB_SANTE_EXCHANGE, VHOST_CLIENT_MAP, messageProperties,
} = require('./rabbit/utils');
const { ModelesRouter } = require('./router/modelesRouter');

class ExpressServer {
  constructor(port) {
    this.port = port;
    this.app = express();
    this.connections = {};
    this.setupMiddleware();
  }

  setupMiddleware() {
    // this.setupAllowedMedia();
    this.app.use(cors());
    this.app.use(bodyParser.json({ limit: '14MB' }));
    this.app.use(express.json());
    this.app.use(express.urlencoded({ extended: false }));
    this.app.use(cookieParser());
    // Simple test to see that the server is up and responding
    this.app.get('/hello', (req, res) => res.send(`Hello World. path: ${this.openApiPath}`));

    this.app.use('/modeles', ModelesRouter);

    // Serve distribution UI
    this.app.use('/', express.static(path.join(__dirname, 'ui')));

    logger.info(`VHOST_CLIENT_MAP: ${VHOST_CLIENT_MAP}`);
    // Subscribe to Hub messages and send them to the client through web socket
    // Get list of keys (corresponding to vhosts) from the VHOSTS map
    const vhostsArray = Object.keys(VHOST_CLIENT_MAP);
    for (const vhost of vhostsArray) {
      connect(vhost, async (connection, channel) => {
        connection.on('error', (err) => {
          logger.error(`Connection error for vhost '${vhost}': ${err}`);
        });

        this.connection = connection;
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
          for (const type of ["message", "ack", "info"]) {
            const queue = `${clientId}.${type}`;
            try {
              channel.consume(queue, (msg) => {
                const body = JSON.parse(msg.content);
                logger.info(` [x] Received for ${clientId} (${vhost}): ${body.distributionID}`);
                logger.debug(` [x] Received for ${clientId} (${vhost}): ${body.distributionID} of content ${msg.content}`);
                const d = new Date();
                const data = {
                  vhost,
                  direction: "←",
                  routingKey: queue,
                  // Ref.: https://stackoverflow.com/a/9849524
                  time: `${d.toLocaleTimeString("fr", { timeZone: "Europe/Paris" }).replace(":", "h")}.${String(new Date().getMilliseconds()).padStart(3, "0")}`,
                  body,
                };
                // Send the message to all connected WebSocket clients
                let clientCounts = 0;
                this.wss.clients.forEach((client) => {
                  if (client.readyState === WebSocket.OPEN) {
                    client.send(JSON.stringify(data));
                    clientCounts += 1;
                  }
                });
                logger.info(`Sent to ${clientCounts} clients: ${data.body.distributionID}`);
                logger.debug(`Sent to ${clientCounts} clients: ${data} of content ${data}`);
              },
              {
                noAck: true, // Ref.: https://amqp-node.github.io/amqplib/channel_api.html#channelconsume
              });
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
    // eslint-disable-next-line no-unused-vars
    this.app.use((err, req, res, next) => {
      // format errors
      res.status(err.status || 500).json({
        message: err.message || err,
        errors: err.errors || '',
      });
    });

    this.server = http.createServer(this.app).listen(this.port);
    this.wss = new WebSocket.Server({ server: this.server });
    // WebSocket server
    this.wss.on('connection', (ws) => {
      logger.info('WebSocket client connected');

      ws.on('message', async (body) => {
        // Publish the message to RabbitMQ
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

module.exports = ExpressServer;
