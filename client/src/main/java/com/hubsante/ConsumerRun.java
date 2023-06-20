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
                "bbo",
                "../certs/users/bbo/fr.test.bbo.p12",
                "trustStore",
                "../certs/trustStore");

        String routingKey = getRouting(args);
        String clientId = getClientId(args);
        String ackRoutingKey = clientId + ".out.ack";
        String languageType = args[1];
        Consumer consumer = new Consumer(HUB_HOSTNAME, HUB_PORT, EXCHANGE_NAME,
                routingKey, ackRoutingKey, clientId) {
            @Override
            protected void deliverCallback(String consumerTag, Delivery delivery) throws IOException {
                String routingKey = delivery.getEnvelope().getRoutingKey();

                EdxlMessage edxlMessage;
                String msgString;

                if(isJsonScheme(languageType)) {
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
                    EdxlMessage ackEdxl = this.generateFunctionalAckMessage(edxlMessage);
                    if (isJsonScheme(languageType)) {
                        this.producerAck.publish(this.fileAckName, ackEdxl);
                    } else {
                        this.producerAck.xmlPublish(this.fileAckName, ackEdxl);
                    }

                    String ackEdxlString = isJsonScheme(languageType) ?
                            mapper.writerWithDefaultPrettyPrinter().writeValueAsString(ackEdxl) :
                            xmlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(ackEdxl);

                    System.out.println("  ↳ [x] Sent  to '" + this.fileAckName + "':'" + ackEdxlString + "'");
                } else {
                    // Inform user that partner has correctly processed the message
                    System.out.println("  ↳ [x] Partner has processed the message.");
                }
            }
        };
        consumer.connect(tlsConf);
        System.out.println(" [*] Waiting for messages on " + routingKey + ". To exit press CTRL+C");
    }
}
