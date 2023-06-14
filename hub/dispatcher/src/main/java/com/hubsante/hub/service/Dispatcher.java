package com.hubsante.hub.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hubsante.hub.config.HubClientConfiguration;
import com.hubsante.model.edxl.DistributionKind;
import com.hubsante.model.edxl.EdxlMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.hubsante.hub.config.AmqpConfiguration.CONSUME_QUEUE_NAME;

@Service
@Slf4j
public class Dispatcher {

    private static final String JSON_CONTENT_TYPE = "application/json";
    private static final String XML_CONTENT_TYPE = "application/xml";

    private final RabbitTemplate rabbitTemplate;
    private final EdxlHandler edxlHandler;
    private final HubClientConfiguration hubConfig;

    public Dispatcher(RabbitTemplate rabbitTemplate, EdxlHandler edxlHandler, HubClientConfiguration hubConfig) {
        this.rabbitTemplate = rabbitTemplate;
        this.edxlHandler = edxlHandler;
        this.hubConfig = hubConfig;
    }

    @RabbitListener(queues = CONSUME_QUEUE_NAME)
    public void dispatch(Message message) {

        String receivedRoutingKey = message.getMessageProperties().getReceivedRoutingKey();
        EdxlMessage edxlMessage;
        String receivedEdxl = new String(message.getBody(), StandardCharsets.UTF_8);

        try {
            // We deserialize according to the content type
            // It MUST be explicitly set by the client
            if (message.getMessageProperties().getContentType().equals(JSON_CONTENT_TYPE)) {
                edxlMessage = edxlHandler.deserializeJsonEDXL(receivedEdxl);
                log.info(" [x] Received from '" + receivedRoutingKey + "':" + edxlHandler.prettyPrintJsonEDXL(edxlMessage));

            } else if (message.getMessageProperties().getContentType().equals(XML_CONTENT_TYPE)) {
                edxlMessage = edxlHandler.deserializeXmlEDXL(receivedEdxl);
                log.info(" [x] Received from '" + receivedRoutingKey + "':" + edxlHandler.prettyPrintXmlEDXL(edxlMessage));

            } else {
                // TODO bbo: determine policy for unknown content type
                throw new AmqpRejectAndDontRequeueException("do not requeue !");
            }

        } catch (IOException e) {
            log.error("Could not parse message " + receivedEdxl + " coming from " + message.getMessageProperties().getConsumerQueue());
            // TODO (bbo) : if we end using a "INFO" channel, we should send an INFO message for this type of errors.
            //  if the message is wrongly formatted client-side we should inform the client.
            //  ----
            //  with Spring Rabbit integration, an exception thrown in a @RabbitListener method will end up with message requeuing
            //  by default, except for AmqpRejectAndDontRequeueException which is specially designed for it. Think about moving it to DLQ instead
            throw new AmqpRejectAndDontRequeueException("do not requeue !");
        }

        String queueType = edxlMessage.getDistributionKind().equals(DistributionKind.ACK) ? "ack" : "message";
        String recipientID = edxlMessage.getDescriptor().getExplicitAddress().getExplicitAddressValue();
        String queueName = recipientID + ".in." + queueType;

        try {
            String senderID = edxlMessage.getSenderID();

            String edxlString = convertToXML(senderID, recipientID) ?
                    edxlHandler.prettyPrintXmlEDXL(edxlMessage) :
                    edxlHandler.prettyPrintJsonEDXL(edxlMessage);

            Message forwardedMsg = new Message(edxlString.getBytes(StandardCharsets.UTF_8), message.getMessageProperties());
            rabbitTemplate.send("", queueName, forwardedMsg);
            log.info("  ↳ [x] Sent to '" + queueName + "':" + edxlString);

        } catch (AmqpException e) {
            // TODO (bbo) : if we catch an AmqpException, ii won't be retried.
            //  We should instead define a retry strategy.
            log.error("[ERROR] Failed to dispatch message " + receivedEdxl + ". Raised exception: " + e);

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean convertToXML(String senderID, String recipientID) {
        // inter forces messaging is always XML
        if (!recipientID.startsWith("fr.health")) {
            return true;
        }
        // for outside -> hubsante messaging, use client preference (default to JSON)
        return !senderID.startsWith("fr.health") &&
                (hubConfig.getClientPreferences().get(recipientID) != null
                        && hubConfig.getClientPreferences().get(recipientID));
    }
}
