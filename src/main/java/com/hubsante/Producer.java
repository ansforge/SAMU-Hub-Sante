package com.hubsante;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;

import java.nio.charset.StandardCharsets;

import static com.hubsante.Utils.getMessage;
import static com.hubsante.Utils.getRouting;

public class Producer {

    private static final String EXCHANGE_NAME = "hubsante";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(5671);
        // Only suitable for development.
        // This code will not perform peer certificate chain verification and prone
        // to man-in-the-middle attacks.
        // See the main TLS guide to learn about peer verification and how to enable it.
        factory.useSslProtocol();
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);

            String routingKey = getRouting(argv);
            String message = getMessage(argv);

            channel.basicPublish(
                    EXCHANGE_NAME,
                    routingKey,
                    MessageProperties.PERSISTENT_TEXT_PLAIN,
                    message.getBytes(StandardCharsets.UTF_8)
            );
            System.out.println(" [x] Sent '" + routingKey + "':'" + message + "'");
        }
    }
}

