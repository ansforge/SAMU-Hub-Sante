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
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import static com.hubsante.hub.config.AmqpConfiguration.*;

@Service
@Slf4j
public class Dispatcher {
    private final RabbitTemplate rabbitTemplate;
    private final EdxlHandler edxlHandler;
    private final HubClientConfiguration hubConfig;

    private static final String HEALTH_PREFIX = "fr.health";

    public Dispatcher(RabbitTemplate rabbitTemplate, EdxlHandler edxlHandler, HubClientConfiguration hubConfig) {
        this.rabbitTemplate = rabbitTemplate;
        this.edxlHandler = edxlHandler;
        this.hubConfig = hubConfig;
    }

    @RabbitListener(queues = CONSUME_QUEUE_NAME)
    public void dispatch(Message message) {
        String receivedRoutingKey = message.getMessageProperties().getReceivedRoutingKey();
        // Deserialize the message according to its content type
        EdxlMessage edxlMessage = deserializeMessage(message);
        // Check that the sender is consistent with the routing key
        checkSenderConsistency(receivedRoutingKey, edxlMessage);
        // Extract recipient queue name from the message (explicit address and distribution kind)
        String queueName = getRecipientQueueName(edxlMessage);
        // Clone the message and adapt properties: set the content type
        Message forwardedMsg = forwardedMessage(edxlMessage, message.getMessageProperties());
        // publish the message to the recipient queue
        rabbitTemplate.send(DISTRIBUTION_EXCHANGE, queueName, forwardedMsg);
    }

    private boolean convertToXML(String senderID, String recipientID) {
        // inter forces messaging is always XML
        if (!recipientID.startsWith(HEALTH_PREFIX)) {
            return true;
        }
        // for outside -> hubsante messaging, use client preference (default to JSON)
        return !senderID.startsWith(HEALTH_PREFIX) &&
                (hubConfig.getClientPreferences().get(recipientID) != null
                        && hubConfig.getClientPreferences().get(recipientID));
    }

    private void checkSenderConsistency(String receivedRoutingKey, EdxlMessage edxlMessage) {
        if (!receivedRoutingKey.equals(edxlMessage.getSenderID())) {
            log.warn("Sender inconsistency for message {} : message sender is {} but received routing key is {}",
                    edxlMessage.getDistributionID(), edxlMessage.getSenderID(), receivedRoutingKey);
            throw new AmqpRejectAndDontRequeueException("do not requeue !");
        }
    }

    private Message forwardedMessage(EdxlMessage edxlMessage, MessageProperties properties) {
        String recipientID = getRecipientID(edxlMessage);
        String senderID = edxlMessage.getSenderID();
        String edxlString;

        overrideExpirationIfNeeded(edxlMessage, properties);

        try {
            if (convertToXML(senderID, recipientID)) {
                edxlString = edxlHandler.prettyPrintXmlEDXL(edxlMessage);
                properties.setContentType(MessageProperties.CONTENT_TYPE_XML);
            } else {
                edxlString = edxlHandler.prettyPrintJsonEDXL(edxlMessage);
                properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
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
        return getRecipientID(edxlMessage) + "." + queueType;
    }

    /*
    ** Deserialize the message according to its content type
     */
    private EdxlMessage deserializeMessage(Message message) {
        String receivedEdxl = new String(message.getBody(), StandardCharsets.UTF_8);
        EdxlMessage edxlMessage;

        try {
            // We deserialize according to the content type
            // It MUST be explicitly set by the client
            if (message.getMessageProperties().getContentType().equals(MessageProperties.CONTENT_TYPE_JSON)) {
                edxlMessage = edxlHandler.deserializeJsonEDXL(receivedEdxl);
                log.info(" [x] Received from '" + message.getMessageProperties().getReceivedRoutingKey() + "':" + edxlHandler.prettyPrintJsonEDXL(edxlMessage));

            } else if (message.getMessageProperties().getContentType().equals(MessageProperties.CONTENT_TYPE_XML)) {
                edxlMessage = edxlHandler.deserializeXmlEDXL(receivedEdxl);
                log.info(" [x] Received from '" + message.getMessageProperties().getReceivedRoutingKey() + "':" + edxlHandler.prettyPrintXmlEDXL(edxlMessage));

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

    private void overrideExpirationIfNeeded(EdxlMessage edxlMessage, MessageProperties properties) {
        // OffsetDateTime comes with seconds and nanos, not millis
        // We assume that one second is an acceptable interval
        long queueExpiration = OffsetDateTime.now().plusSeconds(hubConfig.getDefaultTTL()).toEpochSecond();
        long edxlCustomExpiration = edxlMessage.getDateTimeExpires().toEpochSecond();
        long customDelay = (queueExpiration - edxlCustomExpiration)*1000;

        if (customDelay > 0) {
            properties.setExpiration(String.valueOf(customDelay));
            log.info("override expiration for message {}: expiration is now {}",
                    edxlMessage.getDistributionID(),
                    edxlMessage.getDateTimeExpires().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        }
    }
}
