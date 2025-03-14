const http = require('http');
const express = require('express');
const cors = require('cors');
const cookieParser = require('cookie-parser');
const bodyParser = require('body-parser');
const WebSocket = require('ws');
const logger = require('./logger');
const config = require('./config');

const {
  connect, close, CARTO_CLIENT_ID, VHOST,
} = require('./rabbit/utils');

class ExpressServer {
  constructor(port) {
    this.port = port;
    this.app = express();
    this.connections = {};
    this.setupMiddleware();
    this.setupRoutes();
    this.setupMessaging();
  }

  setupMiddleware() {
    this.app.use(cors());
    this.app.use(bodyParser.json({ limit: '14MB' }));
    this.app.use(express.json());
    this.app.use(express.urlencoded({ extended: false }));
    this.app.use(cookieParser());
  }

  setupRoutes() {
    // Simple test to see that the server is up and responding
    this.app.get('/hello', (req, res) => res.send('Hello World.'));

    this.app.post('/login', (req, res) => {
      const { password } = req.body;
      try {
        // if not valid, return unathorized response
        if (password !== config.POC_USER_SECRET) {
          return res.status(401).json({
            status: 'failed',
            data: [],
            message:
              'Invalid password. Please try again with the correct credentials.',
          });
        }
        // for the POC, only one SAMU49 user with hard data
        return res.status(200).json({
          status: 'success',
          data: {
            name: 'SAMU 49',
            entity: {
              name: 'SAMU 49',
              adress: 'Centre Hospitalier Universitaire Angers Av. de l\'Hôtel Dieu, 49100 Angers',
              lat: 47.48002624511719,
              long: -0.5600687861442566,
              zoom: 10,
            },
          },
          message: 'OK',
        });
      } catch (err) {
        console.log(err);
        return res.status(500).json({
          status: 'error',
          code: 500,
          data: [],
          message: 'Internal Server Error',
        });
      }
    });
  }

  setupMessaging() {
    logger.info(`Carto client id: ${CARTO_CLIENT_ID}`);

    const vhost = VHOST;
    const clientId = CARTO_CLIENT_ID;

    // Subscribe to Hub messages and send them to the client through web socket
    connect(vhost, (connection, channel) => {
      this.connection = connection;
      this.connections[vhost] = connection;

      // eslint-disable-next-line no-restricted-syntax
      for (const type of ['message', 'ack', 'info']) {
        const queue = `${clientId}.${type}`;
        logger.info(` [*] Waiting for ${clientId} messages in ${queue} (${vhost}). To exit press CTRL+C`);
        channel.consume(queue, (msg) => {
          const body = JSON.parse(msg.content);
          logger.info(` [x] Received for ${clientId} (${vhost} ${queue} ${msg.content}): ${body.distributionID}`);
          logger.debug(` [x] Received for ${clientId} (${vhost}): ${body.distributionID} of content ${msg.content}`);
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

      ws.on('close', () => {
        logger.info('WebSocket client disconnected');
      });
    });

    logger.info(`Listening on port ${this.port}`);
  }

  async close() {
    // eslint-disable-next-line no-restricted-syntax
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
