package com.hubsante;

import com.rabbitmq.client.*;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

import static com.hubsante.Utils.getMessageType;

public class Dispatcher {

    private static final String EXCHANGE_NAME = "hubsante";
    private static final String CONSUME_QUEUE_NAME = "*.out.*";

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
        consumeChannel.queueDeclare(CONSUME_QUEUE_NAME, true, false, false, null);

        // ToDo(romainfd): uncomment when multiple consumers are used
        // consumeChannel.basicQos(1); // accept only one unack-ed message at a time

        // Binding to all input queues
        consumeChannel.queueBind(CONSUME_QUEUE_NAME, EXCHANGE_NAME, "*.out.message");
        consumeChannel.queueBind(CONSUME_QUEUE_NAME, EXCHANGE_NAME, "*.out.info");
        consumeChannel.queueBind(CONSUME_QUEUE_NAME, EXCHANGE_NAME, "*.out.ack");

        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String routingKey = delivery.getEnvelope().getRoutingKey();
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println(" [x] Received '" + routingKey + "':'" + message + "'");
            try {
                // Process message
                JSONObject obj = new JSONObject(message);
                String target = obj.getString("to");

                // Dispatch message
                String publishQueueName = target + ".in." + getMessageType(routingKey);
                produceChannel.queueDeclare(publishQueueName, true, false, false, null);
                produceChannel.basicPublish(
                        "",
                        publishQueueName,
                        MessageProperties.PERSISTENT_TEXT_PLAIN,
                        message.getBytes(StandardCharsets.UTF_8)
                );
                System.out.println("  ↳ [x] Sent '" + publishQueueName + "':'" + message + "'");

            } catch (Exception e) {
                // ToDo(romainfd): Better handling of errors (notify sender & receiver?)
                System.out.println("[ERROR] Failed to dispatch message " + message + ". Raised exception: " + e);
            }
            // Sending back technical ack as delivery responsibility was passed to next queue
            consumeChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        };
        consumeChannel.basicConsume(CONSUME_QUEUE_NAME, false, deliverCallback, consumerTag -> {
        });
    }
}

