package com.hubsante.hub.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hubsante.hub.config.HubClientConfiguration;
import com.hubsante.model.edxl.DistributionKind;
import com.hubsante.model.edxl.EdxlMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

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
        String receivedEdxl = new String(message.getBody(), StandardCharsets.UTF_8);

        // Deserialize the message according to its content type
        EdxlMessage edxlMessage = deserializeMessage(receivedEdxl, receivedRoutingKey, message);
        // Check that the sender is consistent with the routing key
        checkSenderConsistency(receivedRoutingKey, edxlMessage);
        // Extract recipient queue name from the message (explicit address and distribution kind)
        String queueName = getRecipientQueueName(edxlMessage);
        // Clone the message and adapt properties: set the content type
        Message forwardedMsg = forwardedMessage(edxlMessage, message.getMessageProperties());
        // publish the message to the recipient queue
        rabbitTemplate.send("", queueName, forwardedMsg);
    }

    private boolean convertToXML(String senderID, String recipientID) {
        // inter forces messaging is always XML
        if (!recipientID.startsWith("fr.health")) {
            return true;
        }
        // for outside -> hubsante messaging, use client preference (default to JSON)
        return !senderID.startsWith("fr.health") &&
                (hubConfig.getClientPreferences().get(recipientID) != null
                        && hubConfig.getClientPreferences().get(recipientID));
    }

    private void checkSenderConsistency(String receivedRoutingKey, EdxlMessage edxlMessage) {
        if (!receivedRoutingKey.startsWith(edxlMessage.getSenderID())) {
            log.warn("Sender inconsistency for message {} : message sender is {} but received routing key is {}",
                    edxlMessage.getDistributionID(), edxlMessage.getSenderID(), receivedRoutingKey);
            throw new AmqpRejectAndDontRequeueException("do not requeue !");
        }
    }

    private Message forwardedMessage(EdxlMessage edxlMessage, MessageProperties properties) {
        String recipientID = getRecipientID(edxlMessage);
        String senderID = edxlMessage.getSenderID();
        String edxlString;

        try {
            if (convertToXML(senderID, recipientID)) {
                edxlString = edxlHandler.prettyPrintXmlEDXL(edxlMessage);
                properties.setContentType(XML_CONTENT_TYPE);
            } else {
                edxlString = edxlHandler.prettyPrintJsonEDXL(edxlMessage);
                properties.setContentType(JSON_CONTENT_TYPE);
            }
            log.info("  ↳ [x] Forwarding to '" + recipientID + "':" + edxlString);
            return new Message(edxlString.getBytes(StandardCharsets.UTF_8), properties);

        } catch (JsonProcessingException e) {
            // For compiler only, this exception is handled previously in the dispatch method
            // because the same methods have already been called
            throw new RuntimeException("Could not serialize message " + edxlMessage.getDistributionID(), e);
        }
    }

    private String getRecipientID(EdxlMessage edxlMessage) {
        return edxlMessage.getDescriptor().getExplicitAddress().getExplicitAddressValue();
    }

    private String getRecipientQueueName(EdxlMessage edxlMessage) {
        String queueType = edxlMessage.getDistributionKind().equals(DistributionKind.ACK) ? "ack" : "message";
        return getRecipientID(edxlMessage) + ".in." + queueType;
    }

    /*
    ** Deserialize the message according to its content type
     */
    private EdxlMessage deserializeMessage(String receivedEdxl, String receivedRoutingKey, Message message) {
        EdxlMessage edxlMessage;

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
                // TODO (bbo) : send message to sender info queue with distributionID and error type ?
                throw new AmqpRejectAndDontRequeueException("do not requeue ! Unhandled message content type : "
                        + message.getMessageProperties().getContentType());
            }

        } catch (JsonProcessingException e) {
            log.error("Could not parse message " + receivedEdxl + " coming from " + message.getMessageProperties().getConsumerQueue(), e);
            // TODO (bbo) : if we end using a "INFO" channel, we should send an INFO message for this type of errors.
            //  if the message is wrongly formatted client-side we should inform the client.
            //  ----
            //  with Spring Rabbit integration, an exception thrown in a @RabbitListener method will end up with message requeuing
            //  by default, except for AmqpRejectAndDontRequeueException which is specially designed for it. Think about moving it to DLQ instead
            throw new AmqpRejectAndDontRequeueException("do not requeue !");
        }
        return edxlMessage;
    }
}
