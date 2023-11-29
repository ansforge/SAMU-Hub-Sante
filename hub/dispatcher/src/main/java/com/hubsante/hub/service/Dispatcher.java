/**
 * Copyright © 2023 Agence du Numerique en Sante (ANS)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hubsante.hub.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.hubsante.hub.config.HubConfiguration;
import com.hubsante.hub.exception.*;
import com.hubsante.model.EdxlHandler;
import com.hubsante.model.Validator;
import com.hubsante.model.edxl.DistributionKind;
import com.hubsante.model.edxl.EdxlEnvelope;
import com.hubsante.model.edxl.EdxlMessage;
import com.hubsante.model.exception.ValidationException;
import com.hubsante.model.report.ErrorCode;
import com.hubsante.model.report.ErrorReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePropertiesBuilder;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import static com.hubsante.hub.config.AmqpConfiguration.*;
import static com.hubsante.hub.utils.EdxlUtils.edxlMessageFromHub;
import static com.hubsante.model.config.Constants.ENVELOPE_SCHEMA;
import static com.hubsante.model.config.Constants.FULL_SCHEMA;

@Service
@Slf4j
public class Dispatcher {
    private final RabbitTemplate rabbitTemplate;
    private final EdxlHandler edxlHandler;
    private final HubConfiguration hubConfig;
    private final Validator validator;

    private static final String HEALTH_PREFIX = "fr.health";

    public Dispatcher(RabbitTemplate rabbitTemplate, EdxlHandler edxlHandler, HubConfiguration hubConfig, Validator validator) {
        this.rabbitTemplate = rabbitTemplate;
        this.edxlHandler = edxlHandler;
        this.hubConfig = hubConfig;
        this.validator = validator;
        initReturnsCallback();
    }

    public void initReturnsCallback() {

        // set returns callback to track undistributed messages
        rabbitTemplate.setReturnsCallback(returned -> {
            EdxlMessage errorEdxlMessage;
            try {
                errorEdxlMessage = edxlHandler.deserializeJsonEDXL(Arrays.toString(returned.getMessage().getBody()));
            } catch ( JsonProcessingException e) {
                // This should never happen as if we've reached this point, the message has already been deserialized
                throw new UnrecognizedMessageFormatException("", null);
            }
            ErrorReport errorReport = new ErrorReport(
                    ErrorCode.UNROUTABLE_MESSAGE,
                    "unable do deliver message to " + returned.getRoutingKey() + ", cause was " + returned.getReplyText() + " (" + returned.getReplyCode() + ")",
                    new String(returned.getMessage().getBody()),
                    errorEdxlMessage.getDistributionID());

            String senderRoutingKey = returned.getMessage().getMessageProperties().getHeader(DLQ_ORIGINAL_ROUTING_KEY);
            logErrorAndSendReport(errorReport, senderRoutingKey);
        });
    }

    @RabbitListener(queues = DISPATCH_QUEUE_NAME)
    public void dispatch(Message message) {
        try {
            // Deserialize the message according to its content type
            EdxlMessage edxlMessage = deserializeMessage(message);
            // Reject the message if the sender is not consistent with the routing key
            checkSenderConsistency(getSenderFromRoutingKey(message), edxlMessage);
            // Reject the message if the delivery mode is not PERSISTENT
            checkDeliveryModeIsPersistent(message, edxlMessage.getDistributionID());
            // Forward the message according to the recipient preferences. Conversion JSON <-> XML can happen here
            Message forwardedMsg = forwardedMessage(edxlMessage, message);
            // Extract recipient queue name from the message (explicit address and distribution kind)
            String queueName = getRecipientQueueName(edxlMessage);
            // publish the message to the recipient queue
            rabbitTemplate.send(DISTRIBUTION_EXCHANGE, queueName, forwardedMsg);
        } catch (AbstractHubException e) {
            handleError(e, message);
        } catch (Exception e) {
            log.error("Unexpected error occurred while dispatching message from " + message.getMessageProperties().getReceivedRoutingKey(), e);
            throw new AmqpRejectAndDontRequeueException(e);
        }
    }

    @RabbitListener(queues = DISPATCH_DLQ_NAME)
    public void dispatchDLQ(Message message) {
        try {
            // TODO bbo
            //  Simple fix to avoid infinite loop if info expires with no header original routing key set
            //  The real fix will be to have two DLQ policies and a specific infoDLQ listener
            String deadFromQueue = message.getMessageProperties().getHeader(DLQ_ORIGINAL_ROUTING_KEY);
            if (deadFromQueue.endsWith(".info")) {
                return;
            }
            EdxlMessage edxlMessage = deserializeMessage(message);
            // log message & error
            String errorCause = "Message " + edxlMessage.getDistributionID() + " has been read from dead-letter-queue; reason was " +
                    message.getMessageProperties().getHeader(DLQ_REASON);
            DeadLetteredMessageException exception = new DeadLetteredMessageException(errorCause, edxlMessage.getDistributionID());
            handleError(exception, message);
        } catch (Exception e) {
            // We don't want to log again the error if it has been thrown by handleError
            // We just log the unexpecteds errors
            if (!(e instanceof AmqpRejectAndDontRequeueException)) {
                String originalRoutingKey = message.getMessageProperties().getHeader(DLQ_ORIGINAL_ROUTING_KEY) != null ?
                        message.getMessageProperties().getHeader(DLQ_ORIGINAL_ROUTING_KEY) : "Unknown routing key";
                log.error("Unexpected error occurred while DLQ-dispatching message from " + originalRoutingKey, e);
            }
            throw new AmqpRejectAndDontRequeueException(e);
        }
    }

    private void handleError(AbstractHubException exception, Message message) {
        // create ErrorReport
        ErrorReport errorReport = new ErrorReport(
                exception.getErrorCode(), exception.getMessage(), new String(message.getBody()), exception.getReferencedDistributionID());

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
        String infoQueueName = getInfoQueueNameFromClientId(sender);

        // log error
        // TODO bbo : add a logback pattern to allow structured logging
        log.error(
                "Error occurred with message published by " + sender + "\n" +
                        "ErrorReport " + errorReport.getErrorCode() + "\n" +
                        "ErrorCause " + errorReport.getErrorCause() + "\n" +
                        "ErrorSourceMessage " + errorReport.getSourceMessage());

        try {
            EdxlMessage errorEdxlMessage = edxlMessageFromHub(sender, errorReport);
            Message errorAmqpMessage;
            if (convertToXML(sender)) {
                errorAmqpMessage = new Message(edxlHandler.serializeXmlEDXL(errorEdxlMessage).getBytes(),
                        MessagePropertiesBuilder.newInstance().setContentType(MessageProperties.CONTENT_TYPE_XML).build());
            } else {
                errorAmqpMessage = new Message(edxlHandler.serializeJsonEDXL(errorEdxlMessage).getBytes(),
                        MessagePropertiesBuilder.newInstance().setContentType(MessageProperties.CONTENT_TYPE_JSON).build());
            }

            rabbitTemplate.send(DISTRIBUTION_EXCHANGE, infoQueueName, errorAmqpMessage);
        } catch (JsonProcessingException e) {
            // This should never happen : we are serializing a POJO with 2 String attributes and a single enum
            log.error("Could not serialize ErrorReport for message " + errorReport.getSourceMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private boolean convertToXML(String recipientID) {
        // sending message to outer hubex is always XML
        if (!recipientID.startsWith(HEALTH_PREFIX)) {
            return true;
        }
        // sending message to health clients is based on client preference (default to JSON)
        return hubConfig.getClientPreferences().get(recipientID) != null
                && hubConfig.getClientPreferences().get(recipientID);
    }

    private void checkSenderConsistency(String receivedRoutingKey, EdxlMessage edxlMessage) {
        if (!receivedRoutingKey.equals(edxlMessage.getSenderID())) {
            String errorCause = "Sender inconsistency for message " +
                    edxlMessage.getDistributionID() +
                    " : message sender is " +
                    edxlMessage.getSenderID() +
                    " but received routing key is " +
                    receivedRoutingKey;
            throw new SenderInconsistencyException(errorCause, edxlMessage.getDistributionID());
        }
    }

    private void checkDeliveryModeIsPersistent(Message message, String messageId) {
        if (!MessageDeliveryMode.PERSISTENT.equals(message.getMessageProperties().getReceivedDeliveryMode())) {
            String errorCause = "Message " + messageId + " has been sent with non-persistent delivery mode";
            throw new DeliveryModeInconsistencyException(errorCause, messageId);
        }
    }

    private Message forwardedMessage(EdxlMessage edxlMessage, Message receivedAmqpMessage) {
        MessageProperties receivedAmqpProperties = receivedAmqpMessage.getMessageProperties();
        MessageProperties forwardedMessageProperties =
                MessagePropertiesBuilder.fromClonedProperties(receivedAmqpProperties).build();

        // we set a per-message TTL if the EDXL.dateTimeExpires is before the queue TTL
        overrideExpirationIfNeeded(edxlMessage, forwardedMessageProperties);
        // we serialize the message according to the recipient preferences
        return getFwdMessageBody(edxlMessage, receivedAmqpMessage, forwardedMessageProperties);
    }

    private String getRecipientID(EdxlMessage edxlMessage) {
        return edxlMessage.getDescriptor().getExplicitAddress().getExplicitAddressValue();
    }

    private String getRecipientQueueName(EdxlMessage edxlMessage) {
        return getRecipientID(edxlMessage) + "." + getQueueType(edxlMessage.getDistributionKind());
    }

    private String getQueueType(DistributionKind kind) {
        switch (kind) {
            case ACK:
                return "ack";
            case ERROR:
                return "info";
            default:
                return "message";
        }
    }

    private String getSenderFromRoutingKey(Message message) {
        return message.getMessageProperties().getReceivedRoutingKey();
    }

    private String getInfoQueueNameFromClientId(String clientId) {
        return clientId + ".info";
    }

    /*
     ** Deserialize the message according to its content type
     */
    private EdxlMessage deserializeMessage(Message message) {
        String receivedEdxl = new String(message.getBody(), StandardCharsets.UTF_8);
        EdxlMessage edxlMessage;

        try {
            edxlMessage = handleMessage(message, receivedEdxl);
        } catch (ValidationException e) {
            // We couldn't validate the message against the full schema, so we try to validate it against the envelope schema
            // so we can at least extract the distributionID
            EdxlEnvelope edxlEnvelope;
            try {
                validator.validateJSON(receivedEdxl, ENVELOPE_SCHEMA);
                edxlEnvelope = edxlHandler.deserializeJsonEDXLEnvelope(receivedEdxl);
            } catch (JsonProcessingException ex) {
                log.error("Could not parse envelope of message " + receivedEdxl + " coming from " + message.getMessageProperties().getReceivedRoutingKey(), e);
                String errorCause = "Could not parse message, invalid format. \n " +
                        "If you don't want to use HubSanté model for now, please use a \"customContent\" wrapper inside your message.";
                throw new UnrecognizedMessageFormatException(errorCause, null);
            } catch (IOException ex) {
                log.error("Could not find schema file", e);
                throw new SchemaNotFoundException("An internal server error has occurred, please contact the administration team", null);
            } catch (ValidationException ex) {
                log.error("Could not validate content or envelope of message " + receivedEdxl + " coming from " + message.getMessageProperties().getReceivedRoutingKey(), e);
                throw new SchemaValidationException(e.getMessage(), null);
            }
            // weird rethrow but we want to log the received routing key and we only have it here
            log.error("Could not validate content of message " + receivedEdxl +
                    " coming from " + message.getMessageProperties().getReceivedRoutingKey() +
                    " with distributionId " + edxlEnvelope.getDistributionID(), e);
            throw new SchemaValidationException(e.getMessage(), edxlEnvelope.getDistributionID());
        }
        return edxlMessage;
    }

    /**
     * Attempts to validate and deserialize the message
     *
     * @param message      the entire message
     * @param receivedEdxl the message's body
     * @return deserialized edxl message
     * @throws JsonProcessingException when deserialization fails
     * @throws ValidationException     when validation fails
     * @throws IOException             when schema file couldn't be found
     */
    private EdxlMessage handleMessage(Message message, String receivedEdxl) throws ValidationException {
        try {
            EdxlMessage edxlMessage;
            // We deserialize according to the content type
            // It MUST be explicitly set by the client
            if (MessageProperties.CONTENT_TYPE_JSON.equals(message.getMessageProperties().getContentType())) {
                validator.validateJSON(receivedEdxl, FULL_SCHEMA);
                edxlMessage = edxlHandler.deserializeJsonEDXL(receivedEdxl);
//                validator.validateContentMessage(edxlMessage, false);

                logMessage(message, edxlMessage);

            } else if (MessageProperties.CONTENT_TYPE_XML.equals(message.getMessageProperties().getContentType())) {
                // TODO bbo: add XSD validation when ready
//                validator.validateXML(receivedEdxl, "edxl/edxl-de-v2.0-wd11.xsd");
                edxlMessage = edxlHandler.deserializeXmlEDXL(receivedEdxl);
//                validator.validateContentMessage(edxlMessage, true);
//                validator.validateXML(receivedEdxl, EDXL_SCHEMA);
                logMessage(message, edxlMessage);

            } else {
                String errorCause = "Unhandled Content-Type ! Message Content-Type should be set at 'application/json' or 'application/xml'";
                throw new NotAllowedContentTypeException(errorCause, null);
            }
            return edxlMessage;
        } catch (JsonProcessingException e) {
            log.error("Could not parse content of message " + receivedEdxl + " coming from " + message.getMessageProperties().getReceivedRoutingKey(), e);
            String errorCause = "Could not parse message, invalid format. \n " +
                    "If you don't want to use HubSanté model for now, please use a \"customContent\" wrapper inside your message.";
            throw new UnrecognizedMessageFormatException(errorCause, null);
        } catch (IOException e) {
            log.error("Could not find schema file", e);
            throw new SchemaNotFoundException("An internal server error has occurred, please contact the administration team", null);
        }
    }

    private void logMessage(Message message, EdxlMessage edxlMessage) throws JsonProcessingException {
        log.info(" [x] Received from '" + message.getMessageProperties().getReceivedRoutingKey() + "': message with distributionID " + edxlMessage.getDistributionID());
        log.debug(edxlHandler.serializeXmlEDXL(edxlMessage));
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
                throw new ExpiredBeforeDispatchMessageException(errorCause, edxlMessage.getDistributionID());
            }
            properties.setExpiration(String.valueOf(newTTL * 1000));
            log.info("override expiration for message {}: expiration is now {}",
                    edxlMessage.getDistributionID(),
                    edxlMessage.getDateTimeExpires().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        }
    }

    private Message getFwdMessageBody(EdxlMessage edxlMessage, Message receivedAmqpMessage, MessageProperties fwdAmqpProperties) {
        String recipientID = getRecipientID(edxlMessage);
        String senderID = getSenderFromRoutingKey(receivedAmqpMessage);
        String edxlString;

        try {
            if (convertToXML(recipientID)) {
                edxlString = edxlHandler.serializeXmlEDXL(edxlMessage);
                fwdAmqpProperties.setContentType(MessageProperties.CONTENT_TYPE_XML);
            } else {
                edxlString = edxlHandler.serializeJsonEDXL(edxlMessage);
                fwdAmqpProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            }
            log.info("  ↳ [x] Forwarding to '" + recipientID + "': message with distributionID " + edxlMessage.getDistributionID());
            log.debug(edxlString);

            fwdAmqpProperties.setHeader(DLQ_ORIGINAL_ROUTING_KEY, senderID);
            return new Message(edxlString.getBytes(StandardCharsets.UTF_8), fwdAmqpProperties);

        } catch (JsonProcessingException e) {
            // For compiler only, this exception is handled previously in the dispatch method
            // because the same methods have already been called in the deserializeMessage method
            throw new RuntimeException("Could not serialize message " + edxlMessage.getDistributionID(), e);
        }
    }
}
