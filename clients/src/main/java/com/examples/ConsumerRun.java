package com.examples;
import com.hubsante.Consumer;
import com.hubsante.TLSConf;
import com.hubsante.model.edxl.DistributionKind;
import com.hubsante.model.edxl.EdxlMessage;
import com.rabbitmq.client.Delivery;
import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.hubsante.Constants.JSON_CONTENT_TYPE;
import static com.hubsante.Constants.TLS_PROTOCOL_VERSION;
import static com.hubsante.Utils.*;

import java.io.IOException;
import java.util.Objects;

public class ConsumerRun {
    private static final Logger logger = LoggerFactory.getLogger(ConsumerRun.class);

    public static void main(String[] args) throws Exception {
        Dotenv dotenv = Dotenv.load();

        TLSConf tlsConf = new TLSConf(
                TLS_PROTOCOL_VERSION,
                dotenv.get("KEY_PASSPHRASE"),
                dotenv.get("CERTIFICATE_PATH"),
                dotenv.get("TRUST_STORE_PASSWORD"),
                dotenv.get("TRUST_STORE_PATH"));

        String queueName = getRouting(args);
        String clientId = getClientId(args);
        Consumer consumer = new Consumer(dotenv.get("HUB_HOSTNAME"), Integer.parseInt(dotenv.get("HUB_PORT")), dotenv.get("VHOST"),
                dotenv.get("EXCHANGE_NAME"),
                queueName, clientId) {
            @Override
            protected void deliverCallback(String consumerTag, Delivery delivery) throws IOException {
                String routingKey = delivery.getEnvelope().getRoutingKey();

                String contentType = delivery.getProperties().getContentType();
                boolean isJsonScheme = Objects.equals(contentType, JSON_CONTENT_TYPE);

                String message = convertBytesToString(delivery.getBody());
                EdxlMessage edxlMessage;
                String stringMessage;

                // STEP 2 - Deserialize received message
                try {
                    if (isJsonScheme) {
                        edxlMessage = edxlHandler.deserializeJsonEDXL(message);
                        stringMessage = edxlHandler.serializeJsonEDXL(edxlMessage);
                    } else {
                        edxlMessage = edxlHandler.deserializeXmlEDXL(message);
                        stringMessage = edxlHandler.serializeXmlEDXL(edxlMessage);
                    }
                } catch (Exception error) {
                    logger.error("[x] Error when receiving message: '"+  error.getMessage());
                    consumeChannel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);

                    return;
                }
                logger.info("[x] Received from '" + routingKey + "':'" + stringMessage + "'");

                // STEP 3 - Send back technical ACK as delivery responsibility is removed from the Hub
                consumeChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

                // STEP 4 - Apply business rules
                // ...
                // If an error occurs, send a message to the "info" queue

                // STEP 5 - Sending back functional ACK to inform that the message has been processed on the Consumer side
                boolean isAckMessage = edxlMessage.getDistributionKind().equals(DistributionKind.ACK);
                if (!isAckMessage) {
                    EdxlMessage ackEdxlMessage = referenceMessageFromReceivedMessage(edxlMessage);
                    if (isJsonScheme) {
                        this.producerAck.publish(this.clientId, ackEdxlMessage);
                    } else {
                        this.producerAck.xmlPublish(this.clientId, ackEdxlMessage);
                    }

                    String ackEdxlString = isJsonScheme ?
                            edxlHandler.serializeJsonEDXL(ackEdxlMessage) :
                            edxlHandler.serializeXmlEDXL(ackEdxlMessage);

                    logger.info("  ↳ [x] Sent  to '" + getExchangeName() + " with routing key " + this.clientId + "':'"
                            + ackEdxlString + "'");
                } else {
                    logger.info("  ↳ [x] Partner has processed the message.");
                }
            }
        };

        // STEP 1 - Connect to Hub
        consumer.connect(tlsConf);
        logger.info(" [*] Waiting for messages on " + queueName + ". To exit press CTRL+C");
    }
}
