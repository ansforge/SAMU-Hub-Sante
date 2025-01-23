package com.examples;

import com.hubsante.Consumer;
import com.hubsante.TLSConf;
import com.hubsante.model.edxl.EdxlMessage;
import com.rabbitmq.client.Delivery;
import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.hubsante.Constants.TLS_PROTOCOL_VERSION;
import static com.hubsante.Utils.*;

public class _02_XmlReceiveMessage {
    private static final Logger logger = LoggerFactory.getLogger(_02_XmlReceiveMessage.class);

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

                // STEP 1 - Convert received EDXL message to string
                String message = convertBytesToString(delivery.getBody());
                EdxlMessage edxlMessage;
                String stringMessage;

                // STEP 2 - Deserialize received message
                edxlMessage = edxlHandler.deserializeXmlEDXL(message);

                // [For demo purposes] stringMessage variable is used to log the received message in the terminal
                stringMessage = edxlHandler.serializeXmlEDXL(edxlMessage);
                logger.info("[x] You have received from '" + routingKey + "':'" + stringMessage + "'");

                // STEP 3 - Send back technical ACK as delivery responsibility is removed from the Hub
                consumeChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

                // STEP 4 - Apply business rules
                // ...
            }
        };

        consumer.connect(tlsConf);
        logger.info(" [*] Waiting for messages on " + queueName + ". To exit press CTRL+C");
    }
}
