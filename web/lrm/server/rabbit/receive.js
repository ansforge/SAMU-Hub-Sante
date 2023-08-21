#!/usr/bin/env node
const { connect } = require('./utils');

const args = process.argv.slice(2);
const queue = (args.length > 0) ? args[0] : 'fr.health.samuB.in.message';
connect((connection, channel) => {
  console.log(' [*] Waiting for messages in %s. To exit press CTRL+C', queue);

  channel.consume(queue, (msg) => {
    console.log(' [x] Received %s', msg.content.toString());
  }, {
    noAck: true, // Ref.: https://amqp-node.github.io/amqplib/channel_api.html#channelconsume
  });
});
