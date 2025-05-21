import { join } from 'path';
import { readFileSync } from 'fs';
import amqp, { credentials, Channel, Connection } from 'amqplib/callback_api';
import { logger } from '../logger';
import { config } from '../config';

const moduleDir = __dirname;

console.log(`Connecting to RabbitMQ server: ${config.getHubUrl()}`);

const opts = {
  // pfx with new encryption needed for Node 19 support
  // Ref: https://github.com/nodejs/node/issues/40672#issuecomment-1680460423
  pfx: readFileSync(join(moduleDir, 'certs/lrm_test.pfx')),
  // cert: fs.readFileSync(path.join(moduleDir, 'certs/local_test.crt')), // client cert
  // key: fs.readFileSync(path.join(moduleDir, 'certs/local_test.key')), // client key
  passphrase: config.getLrmCertPassphrase(),
  ca: [readFileSync(join(moduleDir, 'certs/rootCA.crt'))], // array of trusted CA certs
  // Ref.: https://github.com/amqp-node/amqplib/issues/105
  credentials: credentials.external(),
  clientProperties: { connection_name: 'lrm-interface' },
};

export function connect(vhost: string, callback: (connection: Connection, channel: Channel) => void) {
  amqp.connect(`${config.getHubUrl()}/${vhost}`, opts, (error0, connection) => {
    if (error0) {
      logger.error(`Error during AMQP connection: ${error0}`);
      throw error0;
    }

    connection.createChannel((error1, channel) => {
      if (error1) {
        logger.error(`Error during AMQP channel creation: ${error1}`);
        throw error1;
      }
      callback(connection, channel);
    });
  });
}

export async function connectAsync(vhost: string): Promise<{ connection: Connection; channel: Channel }> {
  return new Promise((resolve, reject) => {
    amqp.connect(`${config.getHubUrl()}/${vhost}`, opts, (error0, connection) => {
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

export function close(connection: Connection, exit = false) {
  setTimeout(() => {
    connection.close();
    if (exit) process.exit(0);
  }, 500);
}
export const messageProperties = {
  // Ref.: https://github.com/amqp-node/amqplib/blob/4791f2dfbe8f3bfbd02bb0907e3c35129ae71c13/lib/api_args.js#L231
  contentType: 'application/json',
  deliveryMode: 2,
  priority: 0,
};
