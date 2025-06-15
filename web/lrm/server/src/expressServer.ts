import { Server as HttpServer, createServer } from 'http';
import cors from 'cors';
import express, { Express } from 'express';
import cookieParser from 'cookie-parser';
import bodyParser from 'body-parser';
import { Server as WssServer } from 'ws';

import { logger } from './logger';
import { RabbitMQConnector } from './rabbit/utils';
import { ModelesRouter } from './router/modelesRouter';
import { Config } from './config';
import { WebSocketHandler } from './WebSocketHandler';
import { MessagingService } from './service/messaging';

export class ExpressServer {
  private readonly config: Config;
  private readonly rabbitMQConnector: RabbitMQConnector;
  private readonly app: Express;
  private readonly connections: MessagingService[];
  private wss: WssServer | undefined;
  private server: HttpServer | undefined;

  constructor(config: Config, connector: RabbitMQConnector) {
    this.config = config;
    this.rabbitMQConnector = connector;
    this.app = express();
    this.connections = [];
    this.setupMiddleware();
  }

  setupMiddleware() {
    this.app.use(cors());
    this.app.use(bodyParser.json({ limit: '14MB' }));
    this.app.use(express.json());
    this.app.use(express.urlencoded({ extended: false }));
    this.app.use(cookieParser());

    this.app.use('/modeles', ModelesRouter);

    // TODO: handle error handling middleware typing properly
    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-expect-error
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    this.app.use((err, req, res, next) => {
      // format errors
      res.status(err.status ?? 500).json({
        message: err.message ?? err,
        errors: err.errors ?? '',
      });
    });
  }

  launch() {
    this.server = createServer(this.app).listen(this.config.getPort());

    this.wss = new WssServer({ server: this.server });
    this.wss.on('connection', (ws) => new WebSocketHandler(ws, this.config, this.rabbitMQConnector));

    logger.info(`Listening on port ${this.config.getPort()}`);

    const VHOST_CLIENT_MAP = this.config.getVhostClientMap();
    // Subscribe to Hub messages and send them to the client through web socket
    logger.info(`VHOST_CLIENT_MAP: ${JSON.stringify(VHOST_CLIENT_MAP)}`);
    // Get list of keys (corresponding to vhosts) from the VHOSTS map
    const vhostsArray = Object.keys(VHOST_CLIENT_MAP);

    for (const vhost of vhostsArray) {
      const service = new MessagingService(vhost, this.config, this.rabbitMQConnector, this.wss);
      service.connectToVhost();
      this.connections.push(service);
    }
  }

  async close() {
    this.connections.forEach((service) => {
      service.handleCloseConnection();
    });
    await new Promise((resolve) => {
      if (this.wss !== undefined) {
        this.wss.close(() => {
          logger.info(`WebSocket closed`);
          resolve(null);
        });
      }
    });
    await new Promise((resolve) => {
      if (this.server !== undefined) {
        this.server.close(() => {
          logger.info(`Server on port ${this.config.getPort()} shut down`);
          resolve(null);
        });
      }
    });
  }
}
