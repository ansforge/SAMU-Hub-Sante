package com.hubsante;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubsante.message.*;
import com.rabbitmq.client.Delivery;

import java.util.Map;
import java.util.UUID;
import java.io.IOException;

import static com.hubsante.Utils.getClientId;
import static com.hubsante.Utils.getRouting;

public class ConsumerRun {

    private static final String EXCHANGE_NAME = "hubsante";
    private static final String HUB_HOSTNAME = "localhost";
    private static final int HUB_PORT = 5671;

    public static void main(String[] args) throws Exception {
        TLSConf tlsConf = new TLSConf(
                "TLSv1.2",
                "certPassword",
                "../certs/client.p12",
                "trustStore",
                "../certs/trustStore");

        String routingKey = getRouting(args);
        String clientId = getClientId(args);
        String ackRoutingKey = clientId + ".out.ack";
        Consumer consumer = new Consumer(HUB_HOSTNAME, HUB_PORT, EXCHANGE_NAME,
                routingKey, ackRoutingKey, clientId) {
            @Override
            protected void deliverCallback(String consumerTag, Delivery delivery) throws IOException {
                String routingKey = delivery.getEnvelope().getRoutingKey();
                ObjectMapper mapper = new ObjectMapper();
                CisuMessage msg = mapper.readValue(delivery.getBody(), CisuMessage.class);
                System.out.println(" [x] Received '" + routingKey + "':'" + msg + "'");

                // Sending back technical ack as delivery responsibility is removed from the Hub
                consumeChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

                // Sending back functional ack as info has been processed on the Consumer side
                if (routingKey.endsWith(".message")) {
                    BasicMessage ackMessage = new BasicMessage(
                            msg.getSenderId(),
                            msg.getTo(),
                            UUID.randomUUID().toString(),
                            Map.of("ackedDistributionId", msg.getDistributionId())
                    );
                    this.producerAck.publish(this.fileAckName, ackMessage);
                    System.out.println("  ↳ [x] Sent '" + this.fileAckName + "':'" + ackMessage + "'");
                } else if (delivery.getEnvelope().getRoutingKey().endsWith(".ack")) {
                    // Inform user that partner has correctly processed the message
                    System.out.println("  ↳ [x] Partner has processed the message.");
                }
            }
        };
        consumer.connect(tlsConf);
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
    }
}
