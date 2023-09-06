package com.hubsante.hub.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hubsante.hub.config.HubClientConfiguration;
import com.hubsante.hub.exception.*;
import com.hubsante.model.edxl.DistributionKind;
import com.hubsante.model.edxl.EdxlMessage;
import com.hubsante.model.report.ErrorCode;
import com.hubsante.model.report.ErrorReport;
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
    private final ContentMessageHandler useCaseHandler;
    private final HubClientConfiguration hubConfig;

    private static final String HEALTH_PREFIX = "fr.health";

    public Dispatcher(RabbitTemplate rabbitTemplate, EdxlHandler edxlHandler, ContentMessageHandler useCaseHandler, HubClientConfiguration hubConfig) {
        this.rabbitTemplate = rabbitTemplate;
        this.edxlHandler = edxlHandler;
        this.useCaseHandler = useCaseHandler;
        this.hubConfig = hubConfig;
    }

    @RabbitListener(queues = DISPATCH_QUEUE_NAME)
    public void dispatch(Message message) {
        try {
            // Deserialize the message according to its content type
            EdxlMessage edxlMessage = deserializeMessage(message);
            // Check if the sender is consistent with the routing key
            checkSenderConsistency(getSenderID(message), edxlMessage);
            // Forward the message according to the recipient preferences. Conversion JSON <-> XML can happen here
            Message forwardedMsg = forwardedMessage(edxlMessage, message);
            // Extract recipient queue name from the message (explicit address and distribution kind)
            String queueName = getRecipientQueueName(edxlMessage);
            // publish the message to the recipient queue
            rabbitTemplate.send(DISTRIBUTION_EXCHANGE, queueName, forwardedMsg);
        } catch (AbstractHubException e) {
            handleError(e, message);
        }
    }

    @RabbitListener(queues = DISPATCH_DLQ_NAME)
    public void dispatchDLQ(Message message) {
        EdxlMessage edxlMessage = deserializeMessage(message);
        // log message & error
        String errorCause = "Message " + edxlMessage.getDistributionID() + " has been read from dead-letter-queue; reason was " +
                message.getMessageProperties().getHeader(DLQ_REASON);
        DeadLetteredMessageException exception = new DeadLetteredMessageException(errorCause);
        handleError(exception, message);
    }

    private void handleError(AbstractHubException exception, Message message) {
        // create ErrorReport
        ErrorReport errorReport = new ErrorReport(
                exception.getErrorCode(), exception.getMessage(), new String(message.getBody()));

        // send ErrorReport to sender
        // if the message has been dead-lettered, we retrieve the original sender from the x-death-original-routing-key header
        String senderClientID = exception instanceof DeadLetteredMessageException ?
                message.getMessageProperties().getHeader(DLQ_ORIGINAL_ROUTING_KEY) :
                message.getMessageProperties().getReceivedRoutingKey();

        logErrorAndSendReport(errorReport, senderClientID);
        // throw exception to reject the message
        throw new AmqpRejectAndDontRequeueException(exception);
    }

    private void logErrorAndSendReport(ErrorReport errorReport, String sender) {
        String infoQueueName = sender + ".info";
        // log error
        // TODO bbo : add a logback pattern to allow structured logging
        log.error(
                "Error occurred with message published by " + sender + "\n" +
                "ErrorReport " + errorReport.getErrorCode() + "\n" +
                        "ErrorCause " + errorReport.getErrorCause() + "\n" +
                        "ErrorSourceMessage " + errorReport.getSourceMessage());

        try {
            rabbitTemplate.send(DISTRIBUTION_EXCHANGE, infoQueueName, new Message(
                    useCaseHandler.serializeJsonMessage(errorReport).getBytes(),
                    // TODO bbo : add a default RabbitTemplate configuration to avoid setting content type for each message
                    //  (only XML ones should be explicitly set)
                    MessagePropertiesBuilder.newInstance().setContentType(MessageProperties.CONTENT_TYPE_JSON).build()));
        } catch (JsonProcessingException e) {
            // This should never happen : we are serializing a POJO with 2 String attributes and a single enum
            log.error("Could not serialize ErrorReport for message " + errorReport.getSourceMessage(), e);
            throw new RuntimeException(e);
        }
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
            String errorCause = "Sender inconsistency for message " +
                    edxlMessage.getDistributionID() +
                    " : message sender is " +
                    edxlMessage.getSenderID() +
                    " but received routing key is " +
                    receivedRoutingKey;
            throw new SenderInconsistencyException(errorCause);
        }
    }

    private Message forwardedMessage(EdxlMessage edxlMessage, Message receivedAmqpMessage) {
        MessageProperties receivedAmqpProperties = receivedAmqpMessage.getMessageProperties();
        MessageProperties forwardedMessageProperties =
                MessagePropertiesBuilder.fromClonedProperties(receivedAmqpProperties).build();

        // we check that the message delivery mode is PERSISTENT and if not,
        // we set it, log the error and send an ErrorReport to the sender
        if (!MessageDeliveryMode.PERSISTENT.equals(receivedAmqpProperties.getReceivedDeliveryMode())) {
            forwardedMessageProperties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            reportDeliveryModeError(receivedAmqpMessage, edxlMessage.getDistributionID());
        }
        // we set a per-message TTL if the EDXL.dateTimeExpires is before the queue TTL
        overrideExpirationIfNeeded(edxlMessage, forwardedMessageProperties);
        // we serialize the message according to the recipient preferences
        return getFwdMessageBody(edxlMessage, receivedAmqpMessage, forwardedMessageProperties);
    }

    private String getRecipientID(EdxlMessage edxlMessage) {
        return edxlMessage.getDescriptor().getExplicitAddress().getExplicitAddressValue();
    }

    private String getRecipientQueueName(EdxlMessage edxlMessage) {
        // TODO bbo : refacto this if we want to allow clients to publish to info queues too.
        String queueType = edxlMessage.getDistributionKind().equals(DistributionKind.ACK) ? "ack" : "message";
        return getRecipientID(edxlMessage) + "." + queueType;
    }

    private String getSenderID(Message message) {
        return message.getMessageProperties().getReceivedRoutingKey();
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
                log.info(" [x] Received from '" + message.getMessageProperties().getReceivedRoutingKey() + "': message with distributionID" + edxlMessage.getDistributionID());
                log.debug(edxlHandler.prettyPrintJsonEDXL(edxlMessage));
            } else if (message.getMessageProperties().getContentType().equals(MessageProperties.CONTENT_TYPE_XML)) {
                edxlMessage = edxlHandler.deserializeXmlEDXL(receivedEdxl);
                log.info(" [x] Received from '" + message.getMessageProperties().getReceivedRoutingKey() + "': message with distributionID " + edxlMessage.getDistributionID());
                log.debug(edxlHandler.prettyPrintXmlEDXL(edxlMessage));
            } else {
                String errorCause = "Unhandled Content-Type ! Message Content-Type should be set at 'application/json' or 'application/xml'";
                throw new NotAllowedContentTypeException(errorCause);
            }

        } catch (JsonProcessingException e) {
            log.error("Could not parse message " + receivedEdxl + " coming from " + message.getMessageProperties().getReceivedRoutingKey(), e);
            String errorCause = "Could not parse message, invalid format. \n " +
                    "If you don't want to use HubSanté model for now, please use a \"customContent\" wrapper inside your message.";
            throw new UnrecognizedMessageFormatException(errorCause);
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
                String errorCause = "Message " + edxlMessage.getDistributionID() + " has expired before reaching the recipient queue";
                throw new ExpiredBeforeDispatchMessageException(errorCause);
            }
            properties.setExpiration(String.valueOf(newTTL * 1000));
            log.info("override expiration for message {}: expiration is now {}",
                    edxlMessage.getDistributionID(),
                    edxlMessage.getDateTimeExpires().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        }
    }

    private void reportDeliveryModeError(Message message, String messageId) {
        // create ErrorReport
        String errorCause = "message " + messageId + "has been received with non-persistent delivery mode";

        ErrorReport errorReport = new ErrorReport(
                ErrorCode.DELIVERY_MODE_INCONSISTENCY,
                errorCause,
                new String(message.getBody()));

        // We do not propagate an exception here because we want to send the message anyway
        // So we call the logErrorAndSendReport method directly
        String senderInfoQueueName = getSenderID(message);
        logErrorAndSendReport(errorReport, senderInfoQueueName);
    }

    private Message getFwdMessageBody(EdxlMessage edxlMessage, Message receivedAmqpMessage, MessageProperties fwdAmqpProperties) {
        String recipientID = getRecipientID(edxlMessage);
        String senderID = getSenderID(receivedAmqpMessage);
        String edxlString;

        try {
            if (convertToXML(senderID, recipientID)) {
                edxlString = edxlHandler.prettyPrintXmlEDXL(edxlMessage);
                fwdAmqpProperties.setContentType(MessageProperties.CONTENT_TYPE_XML);
            } else {
                edxlString = edxlHandler.prettyPrintJsonEDXL(edxlMessage);
                fwdAmqpProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            }
            log.info("  ↳ [x] Forwarding to '" + recipientID + "': message with distributionID " + edxlMessage.getDistributionID());
            log.debug(edxlString);

            fwdAmqpProperties.setHeader(DLQ_ORIGINAL_ROUTING_KEY, getSenderID(receivedAmqpMessage));
            return new Message(edxlString.getBytes(StandardCharsets.UTF_8), fwdAmqpProperties);

        } catch (JsonProcessingException e) {
            // For compiler only, this exception is handled previously in the dispatch method
            // because the same methods have already been called in the deserializeMessage method
            throw new RuntimeException("Could not serialize message " + edxlMessage.getDistributionID(), e);
        }
    }
}
