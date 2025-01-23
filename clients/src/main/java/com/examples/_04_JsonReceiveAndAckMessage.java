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
import static com.hubsante.Utils.referenceMessageFromReceivedMessage;

public class _04_JsonReceiveAndAckMessage {
    private static final Logger logger = LoggerFactory.getLogger(_04_JsonReceiveAndAckMessage.class);

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

                String message = convertBytesToString(delivery.getBody());
                logger.info("[x] Received from '" + routingKey + "':'" + message + "'");

                EdxlMessage edxlMessage;

                try {
                    edxlMessage = edxlHandler.deserializeJsonEDXL(message);
                } catch (Exception error) {
                    logger.error("[x] Error when receiving message: '"+  error.getMessage());
                    consumeChannel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);

                    return;
                }

                consumeChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

                // STEP 1 - Apply business rules
                // ...
                // If an error occurs, send a message to the "info" queue

                // STEP 2 - Sending back functional ACK to inform that the message has been processed on the Consumer side
                if (!isAckMessage(edxlMessage)) {
                    EdxlMessage ackEdxlMessage = referenceMessageFromReceivedMessage(edxlMessage);
                    this.producerAck.publish(this.clientId, ackEdxlMessage);

                    // [For demo purposes] ackEdxlString variable is used to log the message in the terminal
                    String ackEdxlString = edxlHandler.serializeJsonEDXL(ackEdxlMessage);
                    logger.info("  ↳ [x] ACK sent  to '" + getExchangeName() + " with routing key " + this.clientId + "':'"
                            + ackEdxlString + "'");
                } else {
                    logger.info("↳ [x] Partner has processed the message.");
                }
            }
        };

        consumer.connect(tlsConf);
        logger.info(" [*] Waiting for messages on " + queueName + ". To exit press CTRL+C");
    }
}
