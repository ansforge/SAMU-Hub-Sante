#!/usr/bin/env node
const { connect } = require('./utils');
const args = process.argv.slice(2);
const queuePrefix = (args.length > 0) ? args[0] : 'amqps://messaging.hub.esante.gouv.fr:5671/';
const queues = [{key:'15-15',value:'15-15_v1.5'},
  {key:'15-18',value:'15-18_v1.8'},{key:'15-smur',value:'15-smur_v1.4'},{key:'15-gps',value:'15-gps_v1.0'}];
for (const q of queues) {
  connect(q, (connection, channel) => {
    console.log(` [*] Waiting for messages in ${queuePrefix}${q.value}. To exit press CTRL+C`);
    channel.consume(queuePrefix+q.value, (msg) => {
      console.log(` [x] Received ${msg.content.toString()}`);
    }, {
      noAck: true, // Ref.: https://amqp-node.github.io/amqplib/channel_api.html#channelconsume
    });
  });
}
