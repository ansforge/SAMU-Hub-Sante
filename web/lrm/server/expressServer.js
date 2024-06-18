// const { Middleware } = require('swagger-express-middleware');
const http = require('http');
const path = require('path');
const express = require('express');
const cors = require('cors');
const cookieParser = require('cookie-parser');
const bodyParser = require('body-parser');
const WebSocket = require('ws');
const logger = require('./logger');
const {
  connect, connectAsync, close, HUB_SANTE_EXCHANGE, DEMO_CLIENT_IDS, messageProperties,
} = require('./rabbit/utils');

class ExpressServer {
  constructor(port) {
    this.port = port;
    this.app = express();
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

    // Serve distribution UI
    this.app.use('/', express.static(path.join(__dirname, 'ui')));

    // Subscribe to Hub messages and send them to the client through web socket
    connect((connection, channel) => {
      this.connection = connection;
      logger.info("Demo client ids: " + DEMO_CLIENT_IDS)
      const demoIds = JSON.parse(DEMO_CLIENT_IDS);
      logger.info("Parsed JSON: " + demoIds)
      for (const clientKeyValue of demoIds) {
        const clientId = clientKeyValue[0]
        for (const type of ['message', 'ack', 'info']) {
          const queue = `${clientId}.${type}`;
          logger.info(` [*] Waiting for ${clientId} messages in ${queue}. To exit press CTRL+C`);
          channel.consume(queue, (msg) => {
            const body = JSON.parse(msg.content);
            logger.info(` [x] Received for ${clientId}: ${body.distributionID}`);
            logger.debug(` [x] Received for ${clientId}: ${body.distributionID} of content ${msg.content}`);
            const d = new Date();
            const data = {
              direction: '←',
              routingKey: queue,
              // Ref.: https://stackoverflow.com/a/9849524
              time: `${d.toLocaleTimeString('fr', { timeZone: 'Europe/Paris' }).replace(':', 'h')}.${String(new Date().getMilliseconds()).padStart(3, '0')}`,
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
          }, {
            noAck: true, // Ref.: https://amqp-node.github.io/amqplib/channel_api.html#channelconsume
          });
        }
      }
    });
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
        try {
          // Publish the message to RabbitMQ
          const { key, msg } = JSON.parse(body);
          logger.info(`Received message from WebSocket client: ${msg.distributionID}`);
          logger.debug(`Received message from WebSocket client: ${msg.distributionID} of content ${body}`);
          logger.info(` [x] Sending msg ${msg.distributionID} to key ${key}`);
          const { connection, channel } = await connectAsync();
          channel.publish(HUB_SANTE_EXCHANGE, key, Buffer.from(JSON.stringify(msg)), messageProperties);
          close(connection);
          logger.info(`Publish call done and connection closed for ${msg.distributionID}`);
        } catch (error) {
          logger.error(`Error publishing message to RabbitMQ: ${error}`);
        }
      });

      ws.on('close', () => {
        logger.info('WebSocket client disconnected');
      });
    });
    logger.info(`Listening on port ${this.port}`);
  }

  async close() {
    if (this.connection !== undefined) {
      close(this.connection);
      logger.info('RabbitMQ connection shut down');
    }
    if (this.server !== undefined) {
      await this.server.close();
      logger.info(`Server on port ${this.port} shut down`);
    }
  }
}

module.exports = ExpressServer;
