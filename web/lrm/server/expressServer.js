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

// Ref.: https://smallstep.com/hello-mtls/doc/combined/nodejs/axios
/*
const httpsAgent = new https.Agent({
  // Needed to allow self-signed certificates | Ref.: https://stackoverflow.com/a/54903835/10115198
  rejectUnauthorized: false,
  cert: fs.readFileSync('certs/certif.crt'),
  key: fs.readFileSync('certs/certif.key'),
  // ca: fs.readFileSync('certs/ACI-EL-ORG-TEST.crt'),
});
 */

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
    this.app.use('/ui', express.static(path.join(__dirname, 'ui')));

    // Subscribe to Hub messages and send them to the client through web socket
    connect((connection, channel) => {
      for (const [clientName, clientId] of Object.entries(DEMO_CLIENT_IDS)) {
        let queue = `${clientId}.message`;
        if (clientName === 'SDIS_Z') {
          queue = `${clientId}.ack`;
        }
        logger.info(' [*] Waiting for %s messages in %s. To exit press CTRL+C', clientName, queue);
        channel.consume(queue, (msg) => {
          logger.info(' [x] Received from %s: %s', clientName, msg.content.toString());
          const d = new Date();
          const data = {
            direction: '←',
            routingKey: queue,
            code: 200,
            time: `${d.toLocaleTimeString().replace(':', 'h')}.${d.getMilliseconds()}`,
            body: JSON.parse(msg.content),
          };
          // Send the message to all connected WebSocket clients
          this.wss.clients.forEach((client) => {
            if (client.readyState === WebSocket.OPEN) {
              logger.info('Sent to clients:', data);
              client.send(JSON.stringify(data));
            }
          });
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
      console.log('WebSocket client connected');

      ws.on('message', async (body) => {
        console.log(`Received message from WebSocket client: ${body}`);
        try {
          // Publish the message to RabbitMQ
          const { key, msg } = JSON.parse(body);
          logger.info(` [x] Sending msg to key ${key}`);
          const { connection, channel } = await connectAsync();
          channel.publish(
            HUB_SANTE_EXCHANGE, key, Buffer.from(JSON.stringify(msg)), messageProperties,
          );
          close(connection);
          logger.info('Publish call done and connection closed.');
        } catch (error) {
          console.error('Error publishing message to RabbitMQ:', error);
        }
      });

      ws.on('close', () => {
        console.log('WebSocket client disconnected');
      });
    });
    logger.info(`Listening on port ${this.port}`);
  }

  async close() {
    if (this.server !== undefined) {
      await this.server.close();
      logger.info(`Server on port ${this.port} shut down`);
    }
  }
}

module.exports = ExpressServer;
