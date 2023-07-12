#!/usr/bin/env node

const amqp = require('amqplib/callback_api');
const fs = require('fs');

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
  // Ref.: https://github.com/amqp-node/amqplib/blob/4791f2dfbe8f3bfbd02bb0907e3c35129ae71c13/lib/api_args.js#L231
  contentType: 'application/json',
  deliveryMode: 2,
};

amqp.connect('amqps://hubsante.esante.gouv.fr', opts, (error0, connection) => {
  if (error0) {
    throw error0;
  }

  connection.createChannel((error1, channel) => {
    if (error1) {
      throw error1;
    }
    const exchange = 'hubsante';
    const args = process.argv.slice(2);
    const key = (args.length > 0) ? args[0] : 'fr.health.samuA.out.message';
    const msgName = args.slice(1).join(' ') || 'samuA_to_samuB.json';
    const msg = fs.readFileSync(msgName);
    /*
    channel.assertExchange(exchange, 'topic', {
      durable: false,
    });
     */
    channel.publish(exchange, key, Buffer.from(msg));
    console.log(" [x] Sent %s: '%s'", key, msg);
  });

  setTimeout(() => {
    connection.close();
    process.exit(0);
  }, 500);
});
