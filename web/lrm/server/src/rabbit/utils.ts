import { join } from 'path';
import { readFileSync } from 'fs';
import amqp, { credentials, Channel, Connection } from 'amqplib/callback_api';
import { logger } from '../logger';
import { Config } from '../config';
import { Logger } from "winston";

export class RabbitMQConnector {
  private config: Config;
  private connectionOptions: unknown;
  private readonly logger: Logger;

  constructor(config: Config) {
    this.config = config;
    this.connectionOptions = {
      ...this.readCerts(),
      passphrase: this.config.getLrmCertPassphrase(),
      credentials: credentials.external(),
      clientProperties: { connection_name: 'lrm-interface' },
    };
    this.logger = logger.child({ component: 'RabbitMQConnector' });
  }

  private readCerts(): { pfx: Buffer<ArrayBufferLike>; ca: Buffer<ArrayBufferLike>[] } {
    const moduleDir = __dirname;
    return {
      // pfx with new encryption needed for Node 19 support
      // Ref: https://github.com/nodejs/node/issues/40672#issuecomment-1680460423
      pfx: readFileSync(join(moduleDir, 'certs/lrm_test.pfx')),
      // cert: fs.readFileSync(path.join(moduleDir, 'certs/local_test.crt')), // client cert
      // key: fs.readFileSync(path.join(moduleDir, 'certs/local_test.key')), // client key
      ca: [readFileSync(join(moduleDir, 'certs/rootCA.crt'))], // array of trusted CA certs
      // Ref.: https://github.com/amqp-node/amqplib/issues/105
    };
  }

  public connect(vhost: string, callback: (connection: Connection, channel: Channel) => void) {
    amqp.connect(`${this.config.getHubUrl()}/${vhost}`, this.connectionOptions, (error0, connection) => {
      if (error0) {
        this.logger.error(`Error during AMQP connection: ${error0}`);
        throw error0;
      }

      connection.createChannel((error1, channel) => {
        if (error1) {
          this.logger.error(`Error during AMQP channel creation: ${error1}`);
          throw error1;
        }
        callback(connection, channel);
      });
    });
  }

  public async connectAsync(vhost: string): Promise<{ connection: Connection; channel: Channel }> {
    return new Promise((resolve, reject) => {
      amqp.connect(`${this.config.getHubUrl()}/${vhost}`, this.connectionOptions, (error0, connection) => {
        if (error0) {
          reject(error0);
          return;
        }

        connection.createChannel((error1, channel) => {
          if (error1) {
            reject(error1);
            return;
          }

          resolve({ connection, channel });
        });
      });
    });
  }

  public close(connection: Connection, exit = false) {
    setTimeout(() => {
      connection.close();
      if (exit) process.exit(0);
    }, 500);
  }
}
