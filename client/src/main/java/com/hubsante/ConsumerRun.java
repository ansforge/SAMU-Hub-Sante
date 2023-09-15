package com.hubsante;

import com.hubsante.model.edxl.DistributionKind;
import com.hubsante.model.edxl.EdxlMessage;
import com.rabbitmq.client.Delivery;

import java.io.IOException;

import static com.hubsante.Utils.*;

public class ConsumerRun {

    private static final String EXCHANGE_NAME = "hubsante";
    private static final String HUB_HOSTNAME = "hubsante.esante.gouv.fr";
    private static final int HUB_PORT = 5671;

    public static void main(String[] args) throws Exception {
        TLSConf tlsConf = new TLSConf(
                "TLSv1.2",
                "certPassword",
                "../certs/local_test.p12",
                "trustStore",
                "../certs/trustStore");

        String queueName = getRouting(args);
        String clientId = getClientId(args);
        boolean isJsonScheme = "json".equalsIgnoreCase(args[1]);
        Consumer consumer = new Consumer(HUB_HOSTNAME, HUB_PORT, EXCHANGE_NAME,
                queueName, clientId) {
            @Override
            protected void deliverCallback(String consumerTag, Delivery delivery) throws IOException {
                String routingKey = delivery.getEnvelope().getRoutingKey();

                EdxlMessage edxlMessage;
                String msgString;

                if(isJsonScheme) {
                    edxlMessage = this.mapper.readValue(delivery.getBody(), EdxlMessage.class);
                    msgString = this.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(edxlMessage);
                } else {
                    edxlMessage = this.xmlMapper.readValue(delivery.getBody(), EdxlMessage.class);
                    msgString = this.xmlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(edxlMessage);
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
