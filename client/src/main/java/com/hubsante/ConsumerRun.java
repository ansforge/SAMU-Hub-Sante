package com.hubsante;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hubsante.message.*;
import com.rabbitmq.client.Delivery;

import java.io.IOException;

import static com.hubsante.Utils.*;

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
                // registering time module is mandatory to handle date times
                ObjectMapper mapper = new ObjectMapper()
                        .registerModule(new JavaTimeModule())
                        .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);

                BasicMessage basicMessage = mapper.readValue(delivery.getBody(), BasicMessage.class);
                CisuMessage cisuMessage = convertMessageFromType(mapper, basicMessage, delivery.getBody());

                System.out.println(" [x] Received '" + routingKey + "':'" + cisuMessage + "'");

                // Sending back technical ack as delivery responsibility is removed from the Hub
                consumeChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

                // Sending back functional ack as info has been processed on the Consumer side
                if (!basicMessage.getMsgType().getValue().equals("ACK")) {
                    AckMessage ackMessage = this.generateFunctionalAckMessage(basicMessage);
                    this.producerAck.publish(this.fileAckName, ackMessage);
                    System.out.println("  ↳ [x] Sent '" + this.fileAckName + "':'" + ackMessage + "'");
                } else {
                    // Inform user that partner has correctly processed the message
                    System.out.println("  ↳ [x] Partner has processed the message.");
                }
            }
        };
        consumer.connect(tlsConf);
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
    }
}
