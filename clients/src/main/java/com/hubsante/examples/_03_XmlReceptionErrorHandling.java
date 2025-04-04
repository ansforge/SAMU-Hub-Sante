package com.hubsante.examples;

import com.hubsante.Consumer;
import com.hubsante.TLSConf;
import com.rabbitmq.client.Delivery;
import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.hubsante.Constants.TLS_PROTOCOL_VERSION;
import static com.hubsante.Utils.*;

public class _03_XmlReceptionErrorHandling {
    private static final Logger logger = LoggerFactory.getLogger(_03_XmlReceptionErrorHandling.class);

    public static void main(String[] args) throws Exception {
        final Dotenv dotenv = Dotenv.load();

        final TLSConf tlsConf = new TLSConf(
                TLS_PROTOCOL_VERSION,
                dotenv.get("KEY_PASSPHRASE"),
                dotenv.get("CERTIFICATE_PATH"),
                dotenv.get("TRUST_STORE_PASSWORD"),
                dotenv.get("TRUST_STORE_PATH"));

        final String queueName = getRouting(args);
        final String clientId = getClientId(args);

        final Consumer consumer = new Consumer(dotenv.get("HUB_HOSTNAME"), Integer.parseInt(dotenv.get("HUB_PORT")), dotenv.get("VHOST"),
                dotenv.get("EXCHANGE_NAME"),
                queueName, clientId) {
            @Override
            protected void deliverCallback(String consumerTag, Delivery delivery) throws IOException {
                final String routingKey = delivery.getEnvelope().getRoutingKey();

                final String message = convertBytesToString(delivery.getBody());
                logger.info("[x] Received from '" + routingKey + "':'" + message + "'");

                try {
                    edxlHandler.deserializeXmlEDXL(message);
                } catch (IOException error) {
                    logger.error("[x] Error when receiving message: '"+  error.getMessage());

                    // Send back technical non ACK to RabbitMQ as delivery responsibility is removed from the Hub
                    consumeChannel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);

                    return;
                }

                consumeChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

                // Apply business rules
                // ...
                // If an error occurs, send a message to the "info" queue

            }
        };

        consumer.connect(tlsConf);
        logger.info(" [*] Waiting for messages on " + queueName + ". To exit press CTRL+C");
    }

}
