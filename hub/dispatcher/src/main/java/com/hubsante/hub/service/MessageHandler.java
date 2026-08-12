/**
 * Copyright © 2023-2026 Agence du Numerique en Sante (ANS)
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

import static com.hubsante.hub.config.AmqpConfiguration.*;
import static com.hubsante.hub.config.Constants.*;
import static com.hubsante.hub.utils.ConfigUtils.sanitizeVhostForProm;
import static com.hubsante.hub.utils.EdxlUtils.HUB_ID;
import static com.hubsante.hub.utils.EdxlUtils.edxlMessageFromHub;
import static com.hubsante.hub.utils.EdxlUtils.getUseCaseFromMessage;
import static com.hubsante.hub.utils.MessageUtils.*;
import static com.hubsante.model.config.Constants.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.hubsante.hub.config.HubConfiguration;
import com.hubsante.hub.config.LogConstants;
import com.hubsante.hub.config.StructuredLogger;
import com.hubsante.hub.exception.*;
import com.hubsante.hub.utils.ConversionUtils;
import com.hubsante.hub.utils.EdxlUtils;
import com.hubsante.model.EdxlHandler;
import com.hubsante.model.Validator;
import com.hubsante.model.builders.ErrorWrapperBuilder;
import com.hubsante.model.edxl.ContentMessage;
import com.hubsante.model.edxl.DistributionKind;
import com.hubsante.model.edxl.EdxlMessage;
import com.hubsante.model.exception.ValidationException;
import com.hubsante.model.reference.ReferenceWrapper;
import com.hubsante.model.report.Error;
import com.hubsante.model.report.ErrorWrapper;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePropertiesBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MessageHandler {
    private final RabbitTemplate rabbitTemplate;
    private final EdxlHandler edxlHandler;
    private final HubConfiguration hubConfig;
    private final Validator validator;
    private final MeterRegistry registry;

    @Autowired
    @Qualifier("xmlMapper")
    private XmlMapper xmlMapper;

    @Autowired
    @Qualifier("jsonMapper")
    private ObjectMapper jsonMapper;

    private final ConversionHandler conversionHandler;
    private static final StructuredLogger structuredLog = new StructuredLogger(log);

    public MessageHandler(
            RabbitTemplate rabbitTemplate,
            EdxlHandler edxlHandler,
            HubConfiguration hubConfig,
            Validator validator,
            MeterRegistry registry,
            XmlMapper xmlMapper,
            ObjectMapper jsonMapper,
            ConversionHandler conversionHandler) {
        this.rabbitTemplate = rabbitTemplate;
        this.edxlHandler = edxlHandler;
        this.hubConfig = hubConfig;
        this.validator = validator;
        this.registry = registry;
        this.xmlMapper = xmlMapper;
        this.jsonMapper = jsonMapper;
        this.conversionHandler = conversionHandler;
    }

    public HubConfiguration getHubConfig() {
        return hubConfig;
    }

    protected void handleError(AbstractHubException exception, Message message) {
        // create Error
        Error error = new Error();
        error.setErrorCode(exception.getErrorCode());
        error.setErrorCause(exception.getMessage());
        try {
            if (isJSON(message)) {
                error.setSourceMessage(jsonMapper.readValue(message.getBody(), HashMap.class));
            } else if (isXML(message)) {
                error.setSourceMessage(xmlMapper.readValue(message.getBody(), HashMap.class));
            }
        } catch (IOException e) {
            log.error("Could not read message body", e);
        }
        String distributionId = exception.getReferencedDistributionID();
        error.setReferencedDistributionID(distributionId);

        // send Error to sender
        String senderClientId = message.getMessageProperties().getHeader(ORIGINAL_ROUTING_KEY);

        String recipientId = exception.getRecipientId();
        String messageType = exception.getMessageType();

        logErrorMessage(error, distributionId, senderClientId, recipientId, messageType);
        // currently, we do not handle error messages on other hubex, and inhibit DLQ messages other
        // than the "expired" ones
        boolean isSenderInHealthPerimeter = senderClientId.startsWith(FR_HEALTH_PREFIX);
        if (isSenderInHealthPerimeter && !isInhibitedErrorMessage(message)) {
            sendErrorReport(error, senderClientId);
        } else {
            String errorLog =
                    isSenderInHealthPerimeter
                            ? "Error message not sent as it has been dead-letter-queued for an other reason than expiration"
                            : "Error message not sent as it is not a health perimeter";
            structuredLog.info(
                    errorLog,
                    Map.of(
                            LogConstants.SENDER_ID,
                            senderClientId,
                            LogConstants.DISTRIBUTION_ID,
                            distributionId,
                            LogConstants.RECIPIENT_ID,
                            recipientId,
                            LogConstants.MESSAGE_TYPE,
                            messageType));
        }

        // increment metric like dispatch_error{reason="INVALID_MESSAGE",sender="fr.health.samuXXX"}
        publishErrorMetric(exception.getErrorCode().getStatusString(), senderClientId);
        // throw exception to reject the message
        throw new AmqpRejectAndDontRequeueException(exception);
    }

    protected void logErrorMessage(
            Error error,
            String distributionId,
            String senderId,
            String recipientId,
            String messageType) {
        structuredLog.error(
                String.format(
                        "Error occurred with message published by %s \nError %s \nErrorCause %s",
                        senderId, error.getErrorCode(), error.getErrorCause()),
                Map.of(
                        LogConstants.DISTRIBUTION_ID,
                        distributionId,
                        LogConstants.SENDER_ID,
                        senderId,
                        LogConstants.RECIPIENT_ID,
                        recipientId,
                        LogConstants.MESSAGE_TYPE,
                        messageType));
        structuredLog.debug(
                String.format("ErrorSourceMessage was %s", error.getSourceMessage()),
                Map.of(
                        LogConstants.DISTRIBUTION_ID,
                        distributionId,
                        LogConstants.SENDER_ID,
                        senderId,
                        LogConstants.RECIPIENT_ID,
                        recipientId,
                        LogConstants.MESSAGE_TYPE,
                        messageType));
    }

    protected void sendErrorReport(Error error, String sender) {
        String infoQueueName = getInfoQueueNameFromClientId(sender);

        ErrorWrapper wrapper = new ErrorWrapperBuilder(error).build();

        try {
            EdxlMessage errorEdxlMessage = edxlMessageFromHub(sender, wrapper);
            String destinationExchange = DISTRIBUTION_EXCHANGE;
            String routingKey = infoQueueName;

            Message errorAmqpMessage;
            Boolean useXML = hubConfig.getClientPropertiesRegistry().isClientUseXml(sender);

            if (convertToXML(sender, useXML)) {
                errorAmqpMessage =
                        new Message(
                                edxlHandler.serializeXmlEDXL(errorEdxlMessage).getBytes(),
                                MessagePropertiesBuilder.newInstance()
                                        .setContentType(MessageProperties.CONTENT_TYPE_XML)
                                        .build());
            } else {
                errorAmqpMessage =
                        new Message(
                                edxlHandler.serializeJsonEDXL(errorEdxlMessage).getBytes(),
                                MessagePropertiesBuilder.newInstance()
                                        .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                                        .build());

                ConversionUtils.ConversionParametersDTO conversionParameters =
                        ConversionUtils.resolveConversionParameters(hubConfig, errorEdxlMessage);

                if (conversionParameters != null
                        && conversionParameters.conversionType()
                                == ConversionUtils.ConversionType.HEALTH_VERSION_CONVERSION) {
                    List<String> convertedMessages =
                            conversionHandler.applyConversionRules(conversionParameters);
                    if (convertedMessages.size() > 1) {
                        structuredLog.info(
                                "convertedMessages has more than one message: %s",
                                Map.of(
                                        LogConstants.SENDER_ID,
                                        sender,
                                        LogConstants.DISTRIBUTION_ID,
                                        error.getReferencedDistributionID()));
                    }
                    errorAmqpMessage =
                            forwardedStringMessage(convertedMessages.getFirst(), errorAmqpMessage);
                    destinationExchange =
                            ConversionUtils.buildTransferExchangeName(
                                    hubConfig.getVhost(), conversionParameters.targetVhost());
                    routingKey = HUB_ID;
                    structuredLog.info(
                            String.format(
                                    "Message transferred to exchange: %s with routing key: %s",
                                    destinationExchange, routingKey),
                            Map.of(
                                    LogConstants.SENDER_ID,
                                    sender,
                                    LogConstants.DISTRIBUTION_ID,
                                    error.getReferencedDistributionID()));
                }
            }

            rabbitTemplate.send(destinationExchange, routingKey, errorAmqpMessage);
        } catch (JsonProcessingException e) {
            // This should never happen : we are serializing a POJO with 2 String attributes and a
            // single enum
            String distributionId = error.getReferencedDistributionID();
            structuredLog.error(
                    String.format("Could not serialize Error : %s", e.getMessage()),
                    Map.of(
                            LogConstants.SENDER_ID,
                            sender,
                            LogConstants.DISTRIBUTION_ID,
                            distributionId));
            structuredLog.debug(
                    String.format("ErrorSourceMessage was %s", error.getSourceMessage()),
                    Map.of(
                            LogConstants.SENDER_ID,
                            sender,
                            LogConstants.DISTRIBUTION_ID,
                            distributionId));
            throw new RuntimeException(e);
        }
    }

    protected Message forwardedMessage(EdxlMessage edxlMessage, Message receivedAmqpMessage) {
        MessageProperties receivedAmqpProperties = receivedAmqpMessage.getMessageProperties();
        MessageProperties forwardedMessageProperties =
                MessagePropertiesBuilder.fromClonedProperties(receivedAmqpProperties).build();

        // we set a per-message TTL if the EDXL.dateTimeExpires is before the queue TTL
        overrideExpirationIfNeeded(
                edxlMessage, forwardedMessageProperties, hubConfig.getDefaultTTL());
        // we serialize the message according to the recipient preferences
        return getFwdMessageBody(edxlMessage, receivedAmqpMessage, forwardedMessageProperties);
    }

    protected Message forwardedStringMessage(String stringMessage, Message receivedAmqpMessage) {
        MessageProperties receivedAmqpProperties = receivedAmqpMessage.getMessageProperties();
        MessageProperties forwardedMessageProperties =
                MessagePropertiesBuilder.fromClonedProperties(receivedAmqpProperties).build();

        // we serialize the message according to the recipient preferences
        return getFwdStringMessageBody(
                stringMessage, receivedAmqpMessage, forwardedMessageProperties);
    }

    /*
     ** Deserialize the message according to its content type
     */
    protected EdxlMessage extractMessage(Message message) {
        registry.counter(
                        METRIC_MESSAGE_PROCESSING,
                        OPERATION,
                        "extract",
                        VHOST_TAG,
                        sanitizeVhostForProm(hubConfig.getVhost()))
                .increment();
        String receivedEdxl = stringifyBody(message);
        validateFullMessage(message, receivedEdxl);
        return deserializeMessage(message, receivedEdxl);
    }

    private void validateFullMessage(Message message, String receivedEdxl) {
        registry.counter(
                        METRIC_MESSAGE_PROCESSING,
                        OPERATION,
                        "validate.full",
                        VHOST_TAG,
                        sanitizeVhostForProm(hubConfig.getVhost()))
                .increment();
        // We deserialize according to the content type
        // It MUST be explicitly set by the client
        String extractedDistributionId = extractDistributionId(receivedEdxl);
        try {
            if (isJSON(message)) {
                validator.validateJSON(receivedEdxl, FULL_SCHEMA);
            } else if (isXML(message)) {
                validator.validateXML(receivedEdxl, FULL_XSD);
            } else {
                String senderId = message.getMessageProperties().getReceivedRoutingKey();
                structuredLog.error(
                        String.format(
                                "Unhandled Content-Type in message with extracted distributionId %s",
                                extractedDistributionId),
                        Map.of(
                                LogConstants.SENDER_ID,
                                senderId,
                                LogConstants.DISTRIBUTION_ID,
                                extractedDistributionId));
                String errorCause =
                        "Unhandled Content-Type ! Message Content-Type should be set at 'application/json' or 'application/xml'";
                throw new NotAllowedContentTypeException(errorCause, extractedDistributionId);
            }
        } catch (IOException exception) {
            structuredLog.error(
                    "Could not find schema file",
                    Map.of(LogConstants.DISTRIBUTION_ID, extractedDistributionId),
                    exception);
            throw new SchemaNotFoundException(
                    "An internal server error has occurred, please contact the administration team",
                    extractDistributionId(receivedEdxl));
        } catch (ValidationException validationException) {
            validateEnvelopeOnly(message, receivedEdxl);
            throw new SchemaValidationException(
                    validationException.getMessage(), extractedDistributionId);
        }
    }

    private void validateEnvelopeOnly(Message message, String receivedEdxl) {
        registry.counter(
                        METRIC_MESSAGE_PROCESSING,
                        OPERATION,
                        "validate.envelope",
                        VHOST_TAG,
                        sanitizeVhostForProm(hubConfig.getVhost()))
                .increment();
        try {
            String distributionId = UNKNOWN;
            if (isJSON(message)) {
                validator.validateJSON(receivedEdxl, ENVELOPE_SCHEMA);
                distributionId =
                        edxlHandler.deserializeJsonEDXLEnvelope(receivedEdxl).getDistributionID();
            } else if (isXML(message)) {
                validator.validateXML(receivedEdxl, ENVELOPE_XSD);
                distributionId =
                        edxlHandler.deserializeXmlEDXLEnvelope(receivedEdxl).getDistributionID();
            }
            String senderId = message.getMessageProperties().getReceivedRoutingKey();
            structuredLog.error(
                    "Could not validate content of message",
                    Map.of(
                            LogConstants.SENDER_ID,
                            senderId,
                            LogConstants.DISTRIBUTION_ID,
                            distributionId));
            structuredLog.debug(
                    String.format("Received message String was %s", receivedEdxl),
                    Map.of(
                            LogConstants.SENDER_ID,
                            senderId,
                            LogConstants.DISTRIBUTION_ID,
                            distributionId));
        } catch (ValidationException envelopeValidationException) {
            // we replace the ValidationException from the models lib by another one extending
            // AbstractHubException
            String senderId = message.getMessageProperties().getReceivedRoutingKey();
            String distributionId = extractDistributionId(receivedEdxl);
            structuredLog.error(
                    String.format(
                            "Could not validate envelope of message with distributionId possibly being (regex extraction) %s: %s",
                            distributionId, envelopeValidationException.getMessage()),
                    Map.of(
                            LogConstants.SENDER_ID,
                            senderId,
                            LogConstants.DISTRIBUTION_ID,
                            distributionId));
            throw new SchemaValidationException(
                    "CAUTION: distributionId has been extracted by regex because the envelope could not be deserialized.\n"
                            + envelopeValidationException.getMessage(),
                    extractDistributionId(receivedEdxl));
        } catch (IOException exception) {
            String distributionId = extractDistributionId(receivedEdxl);
            structuredLog.error(
                    String.format("Could not find schema file %s", exception.getMessage()),
                    Map.of(LogConstants.DISTRIBUTION_ID, distributionId));
            throw new SchemaNotFoundException(
                    "An internal server error has occurred, please contact the administration team",
                    distributionId);
        }
    }

    private EdxlMessage deserializeMessage(Message message, String receivedEdxl) {
        registry.counter(
                        METRIC_MESSAGE_PROCESSING,
                        OPERATION,
                        "deserialize",
                        VHOST_TAG,
                        sanitizeVhostForProm(hubConfig.getVhost()))
                .increment();
        EdxlMessage edxlMessage;

        // We deserialize according to the content type
        // It MUST be explicitly set by the client
        try {
            if (isJSON(message)) {
                edxlMessage = edxlHandler.deserializeJsonEDXL(receivedEdxl);
                logMessage(message, edxlMessage, receivedEdxl);
            } else if (isXML(message)) {
                edxlMessage = edxlHandler.deserializeXmlEDXL(receivedEdxl);
                logMessage(message, edxlMessage, receivedEdxl);
            } else {
                String distributionId = extractDistributionId(receivedEdxl);
                String errorCause =
                        "Unhandled Content-Type ! Message Content-Type should be set at 'application/json' or 'application/xml'";
                throw new NotAllowedContentTypeException(errorCause, distributionId);
            }
        } catch (JsonProcessingException exception) {
            String senderId = message.getMessageProperties().getReceivedRoutingKey();
            String distributionId = extractDistributionId(receivedEdxl);
            structuredLog.error(
                    String.format("Could not deserialize content: %s", exception),
                    Map.of(
                            LogConstants.SENDER_ID,
                            senderId,
                            LogConstants.DISTRIBUTION_ID,
                            distributionId));
            structuredLog.debug(
                    String.format("Received message String was %s", receivedEdxl),
                    Map.of(
                            LogConstants.SENDER_ID,
                            senderId,
                            LogConstants.DISTRIBUTION_ID,
                            distributionId));
            String errorCause =
                    "An internal server error has occurred, please contact the administration team";
            throw new UnrecognizedMessageFormatException(errorCause, distributionId);
        }
        return edxlMessage;
    }

    private String extractReferencedDistributionID(EdxlMessage edxlMessage) {
        ContentMessage contentMessage = edxlMessage.getFirstContentMessage();
        boolean isAck = DistributionKind.ACK.equals(edxlMessage.getDistributionKind());
        boolean isReferenceWrapper = contentMessage instanceof ReferenceWrapper;
        if (isAck && isReferenceWrapper) {
            return ((ReferenceWrapper) contentMessage).getReference().getDistributionID();
        }
        return null;
    }

    private Message getFwdMessageBody(
            EdxlMessage edxlMessage,
            Message receivedAmqpMessage,
            MessageProperties fwdAmqpProperties) {
        registry.counter(
                        METRIC_MESSAGE_PROCESSING,
                        OPERATION,
                        "serialize.edxl",
                        VHOST_TAG,
                        sanitizeVhostForProm(hubConfig.getVhost()))
                .increment();
        String recipientId = getRecipientID(edxlMessage);
        String senderId = getSenderFromRoutingKey(receivedAmqpMessage);
        String edxlString;
        String distributionKind = edxlMessage.getDistributionKind().getValue();
        String messageType = getUseCaseFromMessage(edxlMessage.getFirstContentMessage());

        try {
            Boolean useXML = hubConfig.getClientPropertiesRegistry().isClientUseXml(recipientId);
            if (convertToXML(recipientId, useXML)) {
                edxlString = edxlHandler.serializeXmlEDXL(edxlMessage);
                fwdAmqpProperties.setContentType(MessageProperties.CONTENT_TYPE_XML);
            } else {
                edxlString = edxlHandler.serializeJsonEDXL(edxlMessage);
                fwdAmqpProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            }

            String distributionId = edxlMessage.getDistributionID();
            String hashedBody = hashBody(receivedAmqpMessage);
            String referencedDistributionId = extractReferencedDistributionID(edxlMessage);
            if (Objects.nonNull(referencedDistributionId)) {
                structuredLog.info(
                        String.format(
                                "Forwarding %s: message with referenced distributionId %s and hashed value %s",
                                distributionKind, referencedDistributionId, hashedBody),
                        Map.of(
                                LogConstants.RECIPIENT_ID,
                                recipientId,
                                LogConstants.DISTRIBUTION_ID,
                                distributionId,
                                LogConstants.SENDER_ID,
                                senderId,
                                LogConstants.MESSAGE_TYPE,
                                messageType));
            } else {
                structuredLog.info(
                        String.format(
                                "Forwarding %s: message with hashed value %s",
                                distributionKind, hashedBody),
                        Map.of(
                                LogConstants.RECIPIENT_ID,
                                recipientId,
                                LogConstants.DISTRIBUTION_ID,
                                distributionId,
                                LogConstants.SENDER_ID,
                                senderId,
                                LogConstants.MESSAGE_TYPE,
                                messageType));
            }
            structuredLog.debug(
                    edxlString,
                    Map.of(
                            LogConstants.RECIPIENT_ID,
                            recipientId,
                            LogConstants.DISTRIBUTION_ID,
                            distributionId,
                            LogConstants.SENDER_ID,
                            senderId,
                            LogConstants.MESSAGE_TYPE,
                            messageType));

            return new Message(edxlString.getBytes(StandardCharsets.UTF_8), fwdAmqpProperties);

        } catch (JsonProcessingException e) {
            // For compiler only, this exception is handled previously in the dispatch method
            // because the same methods have already been called in the deserializeMessage method
            throw new RuntimeException(
                    "Could not serialize message " + edxlMessage.getDistributionID(), e);
        }
    }

    private Message getFwdStringMessageBody(
            String message, Message receivedAmqpMessage, MessageProperties fwdAmqpProperties) {
        registry.counter(
                        METRIC_MESSAGE_PROCESSING,
                        OPERATION,
                        "serialize.string",
                        VHOST_TAG,
                        sanitizeVhostForProm(hubConfig.getVhost()))
                .increment();
        String senderId = getSenderFromRoutingKey(receivedAmqpMessage);
        String distributionId = extractDistributionId(message);

        fwdAmqpProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);

        Message fwdMessage =
                new Message(message.getBytes(StandardCharsets.UTF_8), fwdAmqpProperties);

        structuredLog.info(
                String.format(
                        "Forwarding converted message with hashed value %s", hashBody(fwdMessage)),
                Map.of(
                        LogConstants.SENDER_ID,
                        senderId,
                        LogConstants.DISTRIBUTION_ID,
                        distributionId));

        return fwdMessage;
    }

    protected void inhibitMessageIfNeeded(EdxlMessage edxlMessage) {
        String recipientId = getRecipientID(edxlMessage);
        String useCase = EdxlUtils.getUseCaseFromMessage(edxlMessage.getFirstContentMessage());
        List<String> inhibitedUseCases =
                hubConfig.getClientPropertiesRegistry().getClientInhibitedUseCases(recipientId);

        boolean isInhibited = inhibitedUseCases.contains(useCase);

        if (isInhibited) {
            String errorMessage =
                    String.format(
                            "Use case %s is not supported for client %s", useCase, recipientId);
            throw new UnroutableMessageException(
                    errorMessage, edxlMessage.getDistributionID(), recipientId, useCase);
        }
    }

    private void logMessage(Message message, EdxlMessage edxlMessage, String receivedEdxl) {
        String distributionKind = edxlMessage.getDistributionKind().getValue();
        String senderId = getSenderFromRoutingKey(message);
        String distributionId = edxlMessage.getDistributionID();
        String recipientId = getRecipientID(edxlMessage);
        String referencedDistributionId = extractReferencedDistributionID(edxlMessage);
        String hashedBody = hashBody(message);
        String messageType = getUseCaseFromMessage(edxlMessage.getFirstContentMessage());

        if (Objects.nonNull(referencedDistributionId)) {
            structuredLog.info(
                    String.format(
                            "Received %s: message with referenced distributionId %s and hashed value %s",
                            distributionKind, referencedDistributionId, hashedBody),
                    Map.of(
                            LogConstants.RECIPIENT_ID,
                            recipientId,
                            LogConstants.DISTRIBUTION_ID,
                            distributionId,
                            LogConstants.SENDER_ID,
                            senderId,
                            LogConstants.MESSAGE_TYPE,
                            messageType));
        } else {
            structuredLog.info(
                    String.format(
                            "Received %s: message with hashed value %s",
                            distributionKind, hashedBody),
                    Map.of(
                            LogConstants.RECIPIENT_ID,
                            recipientId,
                            LogConstants.DISTRIBUTION_ID,
                            distributionId,
                            LogConstants.SENDER_ID,
                            senderId,
                            LogConstants.MESSAGE_TYPE,
                            messageType));
        }
        structuredLog.debug(
                receivedEdxl,
                Map.of(
                        LogConstants.RECIPIENT_ID,
                        recipientId,
                        LogConstants.DISTRIBUTION_ID,
                        distributionId,
                        LogConstants.SENDER_ID,
                        senderId,
                        LogConstants.MESSAGE_TYPE,
                        messageType));
    }

    protected void publishErrorMetric(String error, String sender) {
        String editor = hubConfig.getClientPropertiesRegistry().getClientEditor(sender);
        registry.counter(
                        DISPATCH_ERROR,
                        REASON_TAG,
                        error,
                        CLIENT_ID_TAG,
                        sender,
                        VHOST_TAG,
                        sanitizeVhostForProm(hubConfig.getVhost()),
                        EDITOR_TAG,
                        editor)
                .increment();
    }

    protected void publishMetrics(EdxlMessage edxlMessage, Message amqpMessage) {
        String sender = getSenderFromRoutingKey(amqpMessage);
        String useCase = getUseCaseFromMessage(edxlMessage.getFirstContentMessage());
        String editor = hubConfig.getClientPropertiesRegistry().getClientEditor(sender);
        String recipient = getRecipientID(edxlMessage);

        registry.counter(
                        DISPATCHED_MESSAGE,
                        CLIENT_ID_TAG,
                        sender,
                        RECIPIENT_ID_TAG,
                        recipient,
                        VHOST_TAG,
                        sanitizeVhostForProm(hubConfig.getVhost()),
                        USE_CASE_TAG,
                        useCase,
                        EDITOR_TAG,
                        editor)
                .increment();
    }
}
