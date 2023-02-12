package com.hubsante;

import com.rabbitmq.client.*;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public class Dispatcher {

    private static final String EXCHANGE_NAME = "hubsante";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setAutomaticRecoveryEnabled(true);
        if (System.getenv("AMQP_URL") == null){
            factory.setHost("localhost");
        } else {
            // Ref.: https://www.rabbitmq.com/api-guide.html#uri
            factory.setUri(System.getenv("AMQP_URL"));
        }
        Connection connection = null;
        // Retry logic for initial connection | Ref.: https://www.rabbitmq.com/api-guide.html#recovery-triggers
        try {
            connection = factory.newConnection();
        } catch (java.net.ConnectException e) {
            Thread.sleep(5000);
            connection = factory.newConnection();
        }

        // produceChannel: where messages are sent by Hub Santé to be consumed by clients
        Channel produceChannel = connection.createChannel();

        // consumeChannel: where messages are received by Hub Santé and consumed by Dispatcher
        Channel consumeChannel = connection.createChannel();
        consumeChannel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);
        String consumeQueueName = consumeChannel.queueDeclare().getQueue();

        // Binding to all input queues
        consumeChannel.queueBind(consumeQueueName, EXCHANGE_NAME, "*.out.message");
        consumeChannel.queueBind(consumeQueueName, EXCHANGE_NAME, "*.out.info");
        consumeChannel.queueBind(consumeQueueName, EXCHANGE_NAME, "*.out.ack");

        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println(" [x] Received '" + delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");

            // Process message
            JSONObject obj = new JSONObject(message);
            String target = obj.getString("to");

            // Dispatch message
            String publishQueueName = target + ".in.message";
            produceChannel.queueDeclare(publishQueueName, true, false, false, null);
            produceChannel.basicPublish("", publishQueueName, null, message.getBytes(StandardCharsets.UTF_8));
            System.out.println("  ↳ [x] Sent '" + publishQueueName + "':'" + message + "'");
        };
        consumeChannel.basicConsume(consumeQueueName, true, deliverCallback, consumerTag -> {
        });
    }
}

