const path = require('path');
const fs = require('fs');
const amqp = require('amqplib/callback_api');
const logger = require('../logger');

const moduleDir = __dirname;
if (!process.env.HUB_URL) {
  throw new Error('HUB_URL environment variable is not set. In kubernetes, this might be caused by a missing ConfigMap.');
}
const HUB_SANTE_URL = process.env.HUB_URL;
console.log(`Connecting to RabbitMQ server: ${HUB_SANTE_URL}`);
const HUB_SANTE_EXCHANGE = 'hubsante';
const DEMO_CLIENT_IDS = JSON.parse(process.env.CLIENT_MAP);
// ToDo: remove default VHOSTS value once configmap in config are done
const VHOSTS = JSON.parse(process.env.VHOSTS || '["15-15_v1.5", "15-18_v1.8", "15-smur_v1.4", "15-gps_v1.0"]');

const opts = {
  // pfx with new encryption needed for Node 19 support
  // Ref: https://github.com/nodejs/node/issues/40672#issuecomment-1680460423
  pfx: fs.readFileSync(path.join(moduleDir, 'certs/lrm_test.pfx')),
  // cert: fs.readFileSync(path.join(moduleDir, 'certs/local_test.crt')), // client cert
  // key: fs.readFileSync(path.join(moduleDir, 'certs/local_test.key')), // client key
  passphrase: process.env.LRM_PASSPHRASE,
  ca: [fs.readFileSync(path.join(moduleDir, 'certs/rootCA.crt'))], // array of trusted CA certs
  // Ref.: https://github.com/amqp-node/amqplib/issues/105
  credentials: amqp.credentials.external(),
  clientProperties: { connection_name: 'lrm-interface' },
};

module.exports = {
  connect(vhost, callback) {
    amqp.connect(`${HUB_SANTE_URL}/${vhost}`, opts, (error0, connection) => {
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
  async connectAsync(vhost) {
    return new Promise((resolve, reject) => {
      amqp.connect(`${HUB_SANTE_URL}/${vhost}`, opts, (error0, connection) => {
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
  // ToDo: vHost should be passed directly in the message => remove this function
  computeVhostFromMessage(msg) {
    const vhostMap = {
      '15-15_v1.5': ['createCaseHealth', 'createCaseHealthUpdate', 'resourcesInfo', 'resourcesRequest', 'resourcesResponse', 'resourcesStatus'],
      '15-18_v1.8': ['createCase', 'emsi'],
      '15-smur_v1.4': ['rpis'],
      '15-gps_v1.0': ['geoPositionsUpdate', 'geoResourcesDetails', 'geoResourcesRequest'],
    };
    const messageKeys = Object.keys(msg.content[0].jsonContent.embeddedJsonContent.message);
    for (const [vhost, vhostMessages] in Object.entries(vhostMap)) {
      // Check if any of the vhost message keys are present in the current message content keys
      for (const messageKey of messageKeys) {
        if (vhostMessages.includes(messageKey)) {
          return vhost;
        }
      }
    }
    logger.error(`Could not compute vhost from message: no expected message key from ${vhostMap} found in message keys ${messageKeys}`);
    throw new Error('Could not compute vhost from message');
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
  VHOSTS,
};
