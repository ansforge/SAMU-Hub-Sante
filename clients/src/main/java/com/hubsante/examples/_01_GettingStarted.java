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

public class _01_GettingStarted {
    private static final Logger logger = LoggerFactory.getLogger(_01_GettingStarted.class);

    public static void main(String[] args) throws Exception {
        final Dotenv dotenv = Dotenv.load();

        // STEP 1 - Define TLS Configuration to protect connection
        final TLSConf tlsConf = new TLSConf(
                TLS_PROTOCOL_VERSION,
                dotenv.get("KEY_PASSPHRASE"),
                dotenv.get("CERTIFICATE_PATH"),
                dotenv.get("TRUST_STORE_PASSWORD"),
                dotenv.get("TRUST_STORE_PATH"));

        final String queueName = getRouting(args);
        final String clientId = getClientId(args);

        // STEP 2 - Instantiate consumer
        final Consumer consumer = new Consumer(dotenv.get("HUB_HOSTNAME"), Integer.parseInt(dotenv.get("HUB_PORT")), dotenv.get("VHOST"),
                dotenv.get("EXCHANGE_NAME"),
                queueName, clientId) {
            @Override
            // STEP 3 - Define delivery callback, for now, we only log a simple line in the terminal when a message is received
            protected void deliverCallback(String consumerTag, Delivery delivery) throws IOException {
                logger.info("[x] You have received a message from the Hub");

                // STEP 5 - Send back technical ACK as delivery responsibility is removed from the Hub
                consumeChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            }
        };

        // STEP 4 - Connect to Hub
        consumer.connect(tlsConf);
        logger.info(" [*] Waiting for messages on " + queueName + ". To exit press CTRL+C");
    }

}
