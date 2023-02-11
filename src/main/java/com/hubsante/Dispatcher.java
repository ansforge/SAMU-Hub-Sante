package com.hubsante;

import com.rabbitmq.client.*;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public class Dispatcher {

    private static final String EXCHANGE_NAME = "hubsante";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);
        String queueName = channel.queueDeclare().getQueue();

        // Binding to all input queues
        channel.queueBind(queueName, EXCHANGE_NAME, "*.in.message");
        channel.queueBind(queueName, EXCHANGE_NAME, "*.in.info");
        channel.queueBind(queueName, EXCHANGE_NAME, "*.in.ack");

        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println(" [x] Received '" + delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");

            // Process message
            JSONObject obj = new JSONObject(message);
            String target = obj.getString("to");

            // Dispatch message

        };
        channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {
        });
    }
}

