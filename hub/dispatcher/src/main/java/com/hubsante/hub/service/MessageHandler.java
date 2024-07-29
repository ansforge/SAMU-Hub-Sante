/**
 * Copyright © 2023-2024 Agence du Numerique en Sante (ANS)
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.hubsante.hub.config.HubConfiguration;
import com.hubsante.hub.exception.*;
import com.hubsante.hub.spi.*;
import com.hubsante.hub.spi.builders.ErrorWrapperBuilder;
import com.hubsante.hub.spi.exception.ValidationException;
import com.hubsante.hub.spi.report.Error;
import com.hubsante.hub.spi.report.ErrorWrapper;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePropertiesBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static com.hubsante.hub.config.AmqpConfiguration.DISTRIBUTION_EXCHANGE;
import static com.hubsante.hub.config.AmqpConfiguration.DLQ_ORIGINAL_ROUTING_KEY;
import static com.hubsante.hub.config.Constants.*;
import static com.hubsante.hub.spi.config.Constants.*;
import static com.hubsante.hub.utils.EdxlUtils.edxlMessageFromHub;
import static com.hubsante.hub.utils.MessageUtils.*;

@Component
@Slf4j
public class MessageHandler {
    private final RabbitTemplate rabbitTemplate;
    private final EdxlHandlerInterface edxlHandler;
    private final HubConfiguration hubConfig;
    private final ValidatorInterface validator;
    private final MeterRegistry registry;
    @Autowired
    @Qualifier("xmlMapper")
    private XmlMapper xmlMapper;
    @Autowired
    @Qualifier("jsonMapper")
    private ObjectMapper jsonMapper;


    public MessageHandler(RabbitTemplate rabbitTemplate, EdxlHandlerInterface edxlHandler, HubConfiguration hubConfig, ValidatorInterface validator, MeterRegistry registry, XmlMapper xmlMapper, ObjectMapper jsonMapper){
        this.rabbitTemplate = rabbitTemplate;
        this.edxlHandler = edxlHandler;
        this.hubConfig = hubConfig;
        this.validator = validator;
        this.registry = registry;
        this.xmlMapper = xmlMapper;
        this.jsonMapper = jsonMapper;
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
        error.setReferencedDistributionID(exception.getReferencedDistributionID());

        // send Error to sender
        // if the message has been dead-lettered, we retrieve the original sender from the x-death-original-routing-key header
        String senderClientID = exception instanceof DeadLetteredMessageException ?
                message.getMessageProperties().getHeader(DLQ_ORIGINAL_ROUTING_KEY) :
                message.getMessageProperties().getReceivedRoutingKey();

        logErrorAndSendReport(error, senderClientID);
        // increment metric like dispatch_error{reason="INVALID_MESSAGE",sender="fr.health.samuXXX"}
        publishErrorMetric(exception.getErrorCode().getStatusString(), senderClientID);
        // throw exception to reject the message
        throw new AmqpRejectAndDontRequeueException(exception);
    }
    protected void logErrorAndSendReport(Error error, String sender) {
        String infoQueueName = getInfoQueueNameFromClientId(sender);

        // log error
        // TODO bbo : add a logback pattern to allow structured logging
        log.error(
                "Error occurred with message published by " + sender + "\n" +
                        "Error " + error.getErrorCode() + "\n" +
                        "ErrorCause " + error.getErrorCause() + "\n" +
                        "ErrorSourceMessage " + error.getSourceMessage());

        ErrorWrapper wrapper = new ErrorWrapperBuilder(error).build();

        try {
            EdxlMessageInterface errorEdxlMessage = edxlMessageFromHub(sender, wrapper);
            Message errorAmqpMessage;
            if (convertToXML(sender, hubConfig.getClientPreferences().get(sender))) {
                errorAmqpMessage = new Message(edxlHandler.serializeXmlEDXL(errorEdxlMessage).getBytes(),
                        MessagePropertiesBuilder.newInstance().setContentType(MessageProperties.CONTENT_TYPE_XML).build());
            } else {
                errorAmqpMessage = new Message(edxlHandler.serializeJsonEDXL(errorEdxlMessage).getBytes(),
                        MessagePropertiesBuilder.newInstance().setContentType(MessageProperties.CONTENT_TYPE_JSON).build());
            }

            rabbitTemplate.send(DISTRIBUTION_EXCHANGE, infoQueueName, errorAmqpMessage);
        } catch (JsonProcessingException e) {
            // This should never happen : we are serializing a POJO with 2 String attributes and a single enum
            log.error("Could not serialize Error for message " + error.getSourceMessage(), e);
            throw new RuntimeException(e);
        }
    }

    protected Message forwardedMessage(EdxlMessageInterface edxlMessage, Message receivedAmqpMessage) {
        MessageProperties receivedAmqpProperties = receivedAmqpMessage.getMessageProperties();
        MessageProperties forwardedMessageProperties =
                MessagePropertiesBuilder.fromClonedProperties(receivedAmqpProperties).build();

        // we set a per-message TTL if the EDXL.dateTimeExpires is before the queue TTL
        overrideExpirationIfNeeded(edxlMessage, forwardedMessageProperties, hubConfig.getDefaultTTL());
        // we serialize the message according to the recipient preferences
        return getFwdMessageBody(edxlMessage, receivedAmqpMessage, forwardedMessageProperties);
    }

    /*
     ** Deserialize the message according to its content type
     */
    @Timed(value = "deserialize.received.message", description = "Deserialize incoming message - include validation")
    protected EdxlMessageInterface deserializeMessage(Message message) {
        String receivedEdxl = new String(message.getBody(), StandardCharsets.UTF_8);
        EdxlMessageInterface edxlMessage;

        try {
            edxlMessage = handleMessage(message, receivedEdxl);
        } catch (ValidationException e) {
            // We couldn't validate the message against the full schema, so we try to validate it against the envelope schema
            // so we can at least extract the distributionID
            EdxlEnvelopeInterface edxlEnvelope;
            try {
                if (isJSON(message)) {
                    validator.validateJSON(receivedEdxl, ENVELOPE_SCHEMA);
                    edxlEnvelope = edxlHandler.deserializeJsonEDXLEnvelope(receivedEdxl);
                } else { // at this point we have already thrown an exception if message's content-type is neither JSON nor XML
                    validator.validateXML(receivedEdxl, ENVELOPE_XSD);
                    edxlEnvelope = edxlHandler.deserializeXmlEDXLEnvelope(receivedEdxl);
                }

            } catch (JsonProcessingException ex) {
                log.error("Could not parse envelope of message " + receivedEdxl + " coming from " + message.getMessageProperties().getReceivedRoutingKey(), e);
                String errorCause = "Could not parse message, invalid format. \n " +
                        "If you don't want to use HubSanté model for now, please use a \"customContent\" wrapper inside your message.";
                throw new UnrecognizedMessageFormatException(errorCause, DISTRIBUTION_ID_UNAVAILABLE);
            } catch (IOException ex) {
                log.error("Could not find schema file", e);
                throw new SchemaNotFoundException("An internal server error has occurred, please contact the administration team", null);
            } catch (ValidationException ex) {
                log.error("Could not validate content or envelope of message " + receivedEdxl + " coming from " + message.getMessageProperties().getReceivedRoutingKey(), e);
                throw new SchemaValidationException(e.getMessage(), DISTRIBUTION_ID_UNAVAILABLE);
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
    private EdxlMessageInterface handleMessage(Message message, String receivedEdxl) throws ValidationException {
        try {
            EdxlMessageInterface edxlMessage;
            // We deserialize according to the content type
            // It MUST be explicitly set by the client
            if (isJSON(message)) {
                validator.validateJSON(receivedEdxl, FULL_SCHEMA);
                edxlMessage = edxlHandler.deserializeJsonEDXL(receivedEdxl);
                logMessage(message, edxlMessage);

            } else if (isXML(message)) {
                // TODO bbo: add XSD validation when ready
                validator.validateXML(receivedEdxl, FULL_XSD);
                edxlMessage = edxlHandler.deserializeXmlEDXL(receivedEdxl);
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
            throw new UnrecognizedMessageFormatException(errorCause, DISTRIBUTION_ID_UNAVAILABLE);
        } catch (IOException e) {
            log.error("Could not find schema file", e);
            throw new SchemaNotFoundException("An internal server error has occurred, please contact the administration team", null);
        }
    }
    @Timed(value = "serialize.forwarded.message", description = "Serialize forwarded message and return new AMQP message")
    private Message getFwdMessageBody(EdxlMessageInterface edxlMessage, Message receivedAmqpMessage, MessageProperties fwdAmqpProperties) {
        String recipientID = getRecipientID(edxlMessage);
        String senderID = getSenderFromRoutingKey(receivedAmqpMessage);
        String edxlString;

        try {
            if (convertToXML(recipientID, hubConfig.getClientPreferences().get(recipientID))) {
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
    private void logMessage(Message message, EdxlMessageInterface edxlMessage) throws JsonProcessingException {
        log.info(" [x] Received from '" + message.getMessageProperties().getReceivedRoutingKey() + "': message with distributionID " + edxlMessage.getDistributionID());
        log.debug(edxlHandler.serializeJsonEDXL(edxlMessage));
    }

    private void publishErrorMetric(String error, String sender) {
        registry.counter(DISPATCH_ERROR, REASON_TAG, error, CLIENT_ID_TAG, sender).increment();
    }

}
