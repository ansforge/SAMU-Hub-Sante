const fs = require('fs');
const amqp = require('amqplib/callback_api');

const RABBIT_URL = 'amqps://hubsante.esante.gouv.fr';
const opts = {
  pfx: fs.readFileSync('certs/local_test.p12'),
  // cert: fs.readFileSync('certs/local_test.crt'), // client cert
  // key: fs.readFileSync('certs/local_test.key'), // client key
  passphrase: 'certPassword', // passphrase for key
  ca: [
    fs.readFileSync('certs/hub.crt'),
    fs.readFileSync('certs/rootCA.crt'),
  ], // array of trusted CA certs
  // Ref.: https://github.com/amqp-node/amqplib/issues/105
  credentials: amqp.credentials.external(),
};

module.exports = {
  connect(callback) {
    amqp.connect(RABBIT_URL, opts, (error0, connection) => {
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
  close(connection) {
    setTimeout(() => {
      connection.close();
      process.exit(0);
    }, 500);
  },
  messageProperties: {
    // Ref.: https://github.com/amqp-node/amqplib/blob/4791f2dfbe8f3bfbd02bb0907e3c35129ae71c13/lib/api_args.js#L231
    contentType: 'application/json',
    deliveryMode: 2,
    priority: 0,
  },
};
