#!/usr/bin/env node
const { connect } = require('./utils');

const args = process.argv.slice(2);
const vhost = (args.length > 0) ? args[0] : '15-15_v1.5';
const queue = (args.length > 1) ? args[1] : 'fr.health.samuA.message';
connect(vhost, (connection, channel) => {
  console.log(' [*] Waiting for messages in %s. To exit press CTRL+C', queue);

  channel.consume(queue, (msg) => {
    console.log(' [x] Received %s', msg.content.toString());
  }, {
    noAck: true, // Ref.: https://amqp-node.github.io/amqplib/channel_api.html#channelconsume
  });
});
