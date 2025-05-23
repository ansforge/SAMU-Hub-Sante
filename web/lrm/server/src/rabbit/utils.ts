import { join } from 'path';
import { readFileSync } from 'fs';
import amqp, { credentials, Channel, Connection } from 'amqplib/callback_api';
import { logger } from '../logger';

const moduleDir = __dirname;
const missingEnvVars = [];

const { HUB_URL, LRM_PASSPHRASE } = process.env;

if (!HUB_URL) {
  missingEnvVars.push('HUB_URL');
}

if (!LRM_PASSPHRASE) {
  missingEnvVars.push('LRM_PASSPHRASE');
}

if (!process.env.VHOST_CLIENT_MAP) {
  missingEnvVars.push('VHOST_CLIENT_MAP');
}

// Check if the environment variables are set, if not, throw an error
if (missingEnvVars.length > 0) {
  throw new Error(
    `The following environment variables are missing: ${missingEnvVars.join(', ')}. In Kubernetes, this might be caused by a missing ConfigMap or Secret.`,
  );
}

console.log(`Connecting to RabbitMQ server: ${HUB_URL}`);
export const HUB_SANTE_EXCHANGE = 'hubsante';
// TODO: handle properly the VHOST_CLIENT_MAP definition check
export const VHOST_CLIENT_MAP = process.env.VHOST_CLIENT_MAP ? JSON.parse(process.env.VHOST_CLIENT_MAP) : {};

const opts = {
  // pfx with new encryption needed for Node 19 support
  // Ref: https://github.com/nodejs/node/issues/40672#issuecomment-1680460423
  pfx: readFileSync(join(moduleDir, 'certs/lrm_test.pfx')),
  // cert: fs.readFileSync(path.join(moduleDir, 'certs/local_test.crt')), // client cert
  // key: fs.readFileSync(path.join(moduleDir, 'certs/local_test.key')), // client key
  passphrase: process.env.LRM_PASSPHRASE,
  ca: [readFileSync(join(moduleDir, 'certs/rootCA.crt'))], // array of trusted CA certs
  // Ref.: https://github.com/amqp-node/amqplib/issues/105
  credentials: credentials.external(),
  clientProperties: { connection_name: 'lrm-interface' },
};

export function connect(vhost: string, callback: (connection: Connection, channel: Channel) => void) {
  amqp.connect(`${HUB_URL}/${vhost}`, opts, (error0, connection) => {
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
    amqp.connect(`${HUB_URL}/${vhost}`, opts, (error0, connection) => {
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
