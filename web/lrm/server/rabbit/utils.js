const path = require('path');
const fs = require('fs');
const amqp = require('amqplib/callback_api');

const moduleDir = __dirname;

const HUB_SANTE_URL = 'amqps://messaging.hub.esante.gouv.fr';
const HUB_SANTE_EXCHANGE = 'hubsante';
const DEMO_CLIENT_IDS = {
  SAMU_A: 'fr.health.samuA', // fr.health.demo.samuA
  SAMU_B: 'fr.health.samuB', // fr.health.demo.samuB
  SDIS_Z: 'fr.fire.nexsis.sdisZ', // fr.health.demo.sdisZ
};

const opts = {
  pfx: fs.readFileSync(path.join(moduleDir, 'certs/local_test.p12')),
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
        throw error0;
      }

      connection.createChannel((error1, channel) => {
        if (error1) {
          throw error1;
        }
        callback(connection, channel);
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
