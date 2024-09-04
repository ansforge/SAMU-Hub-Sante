#!/usr/bin/env node
const fs = require('fs');
const {
  connect, close, HUB_SANTE_EXCHANGE, messageProperties, connectAsync,
} = require('./utils');
const logger = require('../logger');

const args = process.argv.slice(2);
const sendType = (args.length > 0) ? args[0] : 'async'; // cb or async
const vhost = (args.length > 1) ? args[1] : '15-15_v1.5'; // vhost
const key = (args.length > 2) ? args[2] : 'fr.health.samuA';
const msg = fs.readFileSync(args.slice(3).join(' ') || 'empty_test.json');

if (sendType === 'cb') {
  connect(vhost, (connection, channel) => {
    channel.publish(HUB_SANTE_EXCHANGE, key, Buffer.from(msg), messageProperties);
    logger.info(` [x] Sent by cb ${key}: '${msg}'`);
    close(connection, true);
  });
  logger.info('Done by cb.');
} else {
  const asyncCall = async () => {
    const { connection, channel } = await connectAsync(vhost);
    channel.publish(HUB_SANTE_EXCHANGE, key, Buffer.from(msg), messageProperties);
    logger.info(` [x] Sent ${key}: '${msg}'`);
    close(connection);
    logger.info('Done by async.');
  };
  asyncCall();
}
