package com.hubsante.hub.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hubsante.hub.config.HubClientConfiguration;
import com.hubsante.hub.exception.HubExpiredMessageException;
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

    @RabbitListener(queues = DISPATCH_QUEUE_NAME)
    public void dispatch(Message message) {
        String receivedRoutingKey = message.getMessageProperties().getReceivedRoutingKey();
        // Deserialize the message according to its content type
        EdxlMessage edxlMessage = deserializeMessage(message);
        // Check that the sender is consistent with the routing key
        checkSenderConsistency(receivedRoutingKey, edxlMessage);
        // Extract recipient queue name from the message (explicit address and distribution kind)
        String queueName = getRecipientQueueName(edxlMessage);
        // Clone the message and adapt properties: set the content type
        try {
            Message forwardedMsg = forwardedMessage(edxlMessage, message.getMessageProperties());
            // publish the message to the recipient queue
            rabbitTemplate.send(DISTRIBUTION_EXCHANGE, queueName, forwardedMsg);
        } catch (HubExpiredMessageException e) {
            message.getMessageProperties().setHeader(DLQ_REASON, "expired");
            message.getMessageProperties().setHeader(DLQ_MESSAGE_ORIGIN, queueName);
            rabbitTemplate.send(DISTRIBUTION_DLX, queueName, message);
        }
    }

    @RabbitListener(queues = DISPATCH_DLQ_NAME)
    public void dispatchDLQ(Message message) {
        EdxlMessage edxlMessage = deserializeMessage(message);
        String queueName = getSenderInfoQueueName(edxlMessage);
        // log message & error
        log.warn("Message {} has been read from dead-letter-queue; reason was {}",
                edxlMessage.getDistributionID(),
                message.getMessageProperties().getHeader(DLQ_REASON));
        // send info
        //TODO bbo: use Error model
        rabbitTemplate.send(DISTRIBUTION_EXCHANGE, queueName, new Message(
                ("message " + edxlMessage.getDistributionID() + "has not been consumed on "
                        + message.getMessageProperties().getHeader(DLQ_MESSAGE_ORIGIN)
                ).getBytes()));
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
            String errorMessage = "Sender inconsistency for message " +
                    edxlMessage.getDistributionID() +
                    " : message sender is " +
                    edxlMessage.getSenderID() +
                    " but received routing key is " +
                    receivedRoutingKey;
            log.warn(errorMessage);
            rabbitTemplate.send(DISTRIBUTION_EXCHANGE, getSenderInfoQueueName(edxlMessage), new Message(errorMessage.getBytes()));
            throw new AmqpRejectAndDontRequeueException("do not requeue !");
        }
    }

    private Message forwardedMessage(EdxlMessage edxlMessage, MessageProperties properties) {
        String recipientID = getRecipientID(edxlMessage);
        String senderID = edxlMessage.getSenderID();
        String edxlString;

        if (!MessageDeliveryMode.PERSISTENT.equals(properties.getReceivedDeliveryMode())) {
            properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            //TODO bbo: use Error model when available
            rabbitTemplate.send(DISTRIBUTION_EXCHANGE, getSenderInfoQueueName(edxlMessage),
                    new Message(("message " + edxlMessage.getDistributionID() +
                            "has been received with non-persistent delivery mode").getBytes()));
        }
        overrideExpirationIfNeeded(edxlMessage, properties);

        try {
            if (convertToXML(senderID, recipientID)) {
                edxlString = edxlHandler.prettyPrintXmlEDXL(edxlMessage);
                properties.setContentType(MessageProperties.CONTENT_TYPE_XML);
            } else {
                edxlString = edxlHandler.prettyPrintJsonEDXL(edxlMessage);
                properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            }
            log.debug("  ↳ [x] Forwarding to '" + recipientID + "':" + edxlString);
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

    private String getSenderInfoQueueName(EdxlMessage edxlMessage) {
        return edxlMessage.getSenderID() + ".info";
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
                log.debug(" [x] Received from '" + message.getMessageProperties().getReceivedRoutingKey() + "':" + edxlHandler.prettyPrintJsonEDXL(edxlMessage));

            } else if (message.getMessageProperties().getContentType().equals(MessageProperties.CONTENT_TYPE_XML)) {
                edxlMessage = edxlHandler.deserializeXmlEDXL(receivedEdxl);
                log.debug(" [x] Received from '" + message.getMessageProperties().getReceivedRoutingKey() + "':" + edxlHandler.prettyPrintXmlEDXL(edxlMessage));

            } else {
                String queueName = message.getMessageProperties().getReceivedRoutingKey() + ".info";
                rabbitTemplate.send(DISTRIBUTION_EXCHANGE, queueName, new Message(
                        ("Unhandled Content-Type ! Message Content-Type should be set at 'application/json' or 'application/xml'").getBytes()));
                throw new AmqpRejectAndDontRequeueException("do not requeue ! Unhandled message content type : "
                        + message.getMessageProperties().getContentType());
            }

        } catch (JsonProcessingException e) {
            log.error("Could not parse message " + receivedEdxl + " coming from " + message.getMessageProperties().getConsumerQueue(), e);
            String queueName = message.getMessageProperties().getReceivedRoutingKey() + ".info";
            rabbitTemplate.send(DISTRIBUTION_EXCHANGE, queueName, new Message(
                    new String("Could not parse message, invalid format").getBytes()));
            throw new AmqpRejectAndDontRequeueException("do not requeue !");
        } catch (Exception e) {
            e.printStackTrace();
            throw new AmqpRejectAndDontRequeueException("do not requeue !");
        }
        return edxlMessage;
    }

    private void overrideExpirationIfNeeded(EdxlMessage edxlMessage, MessageProperties properties) {
        // OffsetDateTime comes with seconds and nanos, not millis
        // We assume that one second is an acceptable interval
        long queueExpirationDateTime = OffsetDateTime.now().plusSeconds(hubConfig.getDefaultTTL()).toEpochSecond();
        long edxlCustomExpirationDateTime = edxlMessage.getDateTimeExpires().toEpochSecond();

        // if default expiration (now + queue TTl) outlasts edxl.dateTimeExpires,
        // we have to override per-message TTL
        if (queueExpirationDateTime > edxlCustomExpirationDateTime) {
            // if edxl.dateTimeExpires is in the past, we set TTL to 0
            // it would be automatically discarded to DLQ (cf https://www.rabbitmq.com/ttl.html)
            long newTTL = Math.max(0,
                    edxlMessage.getDateTimeExpires().toEpochSecond() - OffsetDateTime.now().toEpochSecond());

            if (newTTL == 0) {
                log.warn("message {} has expired", edxlMessage.getDistributionID());
                throw new HubExpiredMessageException("message " + edxlMessage.getDistributionID() + " has expired");
            }
            properties.setExpiration(String.valueOf(newTTL * 1000));
            log.info("override expiration for message {}: expiration is now {}",
                    edxlMessage.getDistributionID(),
                    edxlMessage.getDateTimeExpires().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        }
    }
}
