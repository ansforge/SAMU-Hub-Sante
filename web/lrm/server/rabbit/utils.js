const path = require('path');
const fs = require('fs');
const amqp = require('amqplib/callback_api');
const logger = require('../logger');
require('dotenv').config();

const moduleDir = __dirname;

const HUB_SANTE_URL = process.env.HUB_URL || 'amqps://messaging.hub.esante.gouv.fr';
const HUB_SANTE_EXCHANGE = 'hubsante';
const DEMO_CLIENT_IDS = {
  SAMU_A: 'fr.health.samuA', // fr.health.demo.samuA
  SAMU_B: 'fr.health.samuB', // fr.health.demo.samuB
  SDIS_Z: 'fr.fire.nexsis.sdisZ', // fr.health.demo.sdisZ
};

const opts = {
  // pfx with new encryption needed for Node 19 support
  // Ref: https://github.com/nodejs/node/issues/40672#issuecomment-1680460423
  pfx: fs.readFileSync(path.join(moduleDir, 'certs/local_test.pfx')),
  // cert: fs.readFileSync(path.join(moduleDir, 'certs/local_test.crt')), // client cert
  // key: fs.readFileSync(path.join(moduleDir, 'certs/local_test.key')), // client key
  passphrase: 'certPassword', // passphrase for key
  ca: [fs.readFileSync(path.join(moduleDir, 'certs/rootCA.crt'))], // array of trusted CA certs
  // Ref.: https://github.com/amqp-node/amqplib/issues/105
  credentials: amqp.credentials.external(),
};

module.exports = {
  connect(callback) {
    amqp.connect(HUB_SANTE_URL, opts, (error0, connection) => {
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
  },
  async connectAsync() {
    return new Promise((resolve, reject) => {
      amqp.connect(HUB_SANTE_URL, opts, (error0, connection) => {
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
  },
  close(connection, exit = false) {
    setTimeout(() => {
      connection.close();
      if (exit) process.exit(0);
    }, 500);
  },
  messageProperties: {
    // Ref.: https://github.com/amqp-node/amqplib/blob/4791f2dfbe8f3bfbd02bb0907e3c35129ae71c13/lib/api_args.js#L231
    contentType: 'application/json',
    deliveryMode: 2,
    priority: 0,
  },
  HUB_SANTE_URL,
  HUB_SANTE_EXCHANGE,
  DEMO_CLIENT_IDS,
};
