package com.hubsante;
import com.hubsante.model.edxl.DistributionKind;
import com.hubsante.model.edxl.EdxlMessage;
import com.rabbitmq.client.Delivery;
import io.github.cdimascio.dotenv.Dotenv;

import static com.hubsante.Utils.*;

import java.io.IOException;

public class ConsumerRun {
    private static final String TLS_PROTOCOL_VERSION = "TLSv1.2";
    private static String TLSProtocolVersion = "TLSv1.2";

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
        boolean isJsonScheme = "json".equalsIgnoreCase(args[1]);
        Consumer consumer = new Consumer(dotenv.get("HUB_HOSTNAME"), Integer.parseInt(dotenv.get("HUB_PORT")), dotenv.get("VHOST"),
                dotenv.get("EXCHANGE_NAME"),
                queueName, clientId) {
            @Override
            protected void deliverCallback(String consumerTag, Delivery delivery) throws IOException {
                String routingKey = delivery.getEnvelope().getRoutingKey();

                EdxlMessage edxlMessage;
                String msgString;

                try {
                    if (isJsonScheme) {
                        edxlMessage = this.mapper.readValue(delivery.getBody(), EdxlMessage.class);
                        msgString = this.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(edxlMessage);
                    } else {
                        edxlMessage = this.xmlMapper.readValue(delivery.getBody(), EdxlMessage.class);
                        msgString = this.xmlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(edxlMessage);
                    }
                } catch (Exception error) {
                    System.out.println(" [x] Error when receiving message:'" + error.getMessage());
                    return;
                }
                System.out.println(" [x] Received from '" + routingKey + "':'" + msgString + "'");

                // Sending back technical ack as delivery responsibility is removed from the Hub
                consumeChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

                // Sending back functional ack as info has been processed on the Consumer side
                if (!edxlMessage.getDistributionKind().equals(DistributionKind.ACK)) {
                    EdxlMessage ackEdxl = referenceMessageFromReceivedMessage(edxlMessage);
                    if (isJsonScheme) {
                        this.producerAck.publish(this.clientId, ackEdxl);
                    } else {
                        this.producerAck.xmlPublish(this.clientId, ackEdxl);
                    }

                    String ackEdxlString = isJsonScheme ?
                            mapper.writerWithDefaultPrettyPrinter().writeValueAsString(ackEdxl) :
                            xmlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(ackEdxl);

                    System.out.println("  ↳ [x] Sent  to '" + getExchangeName() + " with routing key " + this.clientId + "':'"
                            + ackEdxlString + "'");
                } else {
                    // Inform user that partner has correctly processed the message
                    System.out.println("  ↳ [x] Partner has processed the message.");
                }
            }
        };
        consumer.connect(tlsConf);
        System.out.println(" [*] Waiting for messages on " + queueName + ". To exit press CTRL+C");
    }
}
