package com.hubsante;

import com.rabbitmq.client.*;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

import static com.hubsante.Utils.TLS.enableTLS;
import static com.hubsante.Utils.getClientId;
import static com.hubsante.Utils.getRouting;

public class Consumer {
    private static final String EXCHANGE_NAME = "hubsante";

    public static void main(String[] argv) throws Exception {
        String queueName = getRouting(argv);

        ConnectionFactory factory = new ConnectionFactory();
        enableTLS(factory, "certPassword", "certs/client.p12", "trustStore", "certs/trustStore");
        Connection connection = factory.newConnection();

        // consumeChannel: where messages are received by the client from Hub Santé
        Channel consumeChannel = connection.createChannel();
        consumeChannel.queueDeclare(queueName, true, false, false, null);
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        // produceChannel: where ack messages are sent to Hub Santé
        String ackOutRoutingKey = getClientId(argv) + ".out.ack";
        Channel produceChannel = connection.createChannel();
        produceChannel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String routingKey = delivery.getEnvelope().getRoutingKey();
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println(" [x] Received '" + routingKey + "':'" + message + "'");

            // Process message
            JSONObject obj = new JSONObject(message);
            String distributionId = obj.getString("distributionId");
            String senderId = obj.getString("senderId");

            // Sending back technical ack as delivery responsibility is removed from the Hub
            consumeChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

            // Sending back functional ack as info has been processed on the Consumer side
            if (routingKey.endsWith(".message")) {
                Message ackMessage = new Message(senderId, getClientId(argv), distributionId, "Ack");
                produceChannel.basicPublish(
                        EXCHANGE_NAME,
                        ackOutRoutingKey,
                        MessageProperties.PERSISTENT_TEXT_PLAIN,
                        ackMessage.toJsonString().getBytes(StandardCharsets.UTF_8)
                );
                System.out.println("  ↳ [x] Sent '" + ackOutRoutingKey + "':'" + ackMessage.toJsonString() + "'");
            } else if (delivery.getEnvelope().getRoutingKey().endsWith(".ack")) {
                // Inform user that partner has correctly processed the message
                System.out.println("  ↳ [x] Partner has processed the message.");
            }
        };
        consumeChannel.basicConsume(queueName, false, deliverCallback, consumerTag -> { });
    }
}
