#!/usr/bin/env node
const fs = require('fs');
const { connect, close, messageProperties } = require('./utils');

const exchange = 'hubsante';
const args = process.argv.slice(2);
const key = (args.length > 0) ? args[0] : 'fr.health.samuA.out.message';
const msg = fs.readFileSync(args.slice(1).join(' ') || 'samuA_to_samuB.json');

connect((connection, channel) => {
  channel.publish(exchange, key, Buffer.from(msg), messageProperties);
  console.log(" [x] Sent %s: '%s'", key, msg);

  close(connection);
});
