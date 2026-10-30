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
import static com.hubsante.hub.utils.MessageUtils.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.hubsante.hub.config.LogConstants;
import com.hubsante.hub.config.MessageLogContext;
import com.hubsante.hub.config.StructuredLogger;
import com.hubsante.hub.exception.*;
import com.hubsante.hub.service.routing.FireToHubStrategy;
import com.hubsante.hub.service.routing.HubSanteInternalStrategy;
import com.hubsante.hub.service.routing.HubToFireStrategy;
import com.hubsante.hub.service.routing.MessageRoutingDTO;
import com.hubsante.hub.service.routing.RoutingStrategy;
import com.hubsante.hub.utils.ConversionUtils;
import com.hubsante.hub.utils.EdxlUtils;
import com.hubsante.hub.utils.MessageUtils;
import com.hubsante.model.EdxlHandler;
import com.hubsante.model.edxl.EdxlMessage;
import com.hubsante.model.report.Error;
import com.hubsante.model.report.ErrorCode;
import io.micrometer.core.annotation.Timed;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/*
 * This class contains the RabbitMQ logic : the two listeners (Dispatch and DispatchDLQ) and the callback method to handle
 * publisher confirms.
 *
 * The rest of the logic is implemented in two other classes in order to maintain reasonable code size in each file : MessageHandler.java
 * and MessageUtils.java
 *
 * The discrimination between these two classes is pretty basic :
 * - static methods who only need parameters in entry to render an output are stored in MessageUtils
 * - MessageHandler class is annotated @Component to be managed by Spring. It allows it to automatically access other Spring managed components,
 * such as EdxlHandler (deserializer), Validator, RabbitTemplate.
 *
 * Therefore, every method who needs to deserialize, validate, or interact with the RabbitMQ broker should lay in MessageHandler class;
 * every method who doesn't should lay in MessageUtils (checkSenderConsistency, getSenderFromRoutingKey, etc.)
 */
@Service
@Slf4j
public class Dispatcher {

    private final MessageHandler messageHandler;
    private final RabbitTemplate rabbitTemplate;
    private final EdxlHandler edxlHandler;

    @Autowired
    @Qualifier("xmlMapper")
    private XmlMapper xmlMapper;

    @Autowired
    @Qualifier("jsonMapper")
    private ObjectMapper jsonMapper;

    private final ObservabilityService observabilityService;
    private static final StructuredLogger structuredLog = new StructuredLogger(log);
    private final HubSanteInternalStrategy hubSanteInternalStrategy;
    private final HubToFireStrategy hubToFireStrategy;
    private final FireToHubStrategy fireToHubStrategy;

    public Dispatcher(
            MessageHandler messageHandler,
            RabbitTemplate rabbitTemplate,
            EdxlHandler edxlHandler,
            XmlMapper xmlMapper,
            ObjectMapper jsonMapper,
            HubSanteInternalStrategy hubSanteInternalStrategy,
            HubToFireStrategy hubToFireStrategy,
            FireToHubStrategy fireToHubStrategy,
            ObservabilityService observabilityService) {
        this.messageHandler = messageHandler;
        this.rabbitTemplate = rabbitTemplate;
        this.edxlHandler = edxlHandler;
        this.xmlMapper = xmlMapper;
        this.jsonMapper = jsonMapper;
        this.observabilityService = observabilityService;
        this.hubSanteInternalStrategy = hubSanteInternalStrategy;
        this.hubToFireStrategy = hubToFireStrategy;
        this.fireToHubStrategy = fireToHubStrategy;
        initReturnsCallback();
    }

    /**
     * Selects the routing strategy matching the message's sender/recipient cinematic (both health,
     * health to fire, or fire to health).
     */
    public RoutingStrategy selectRoutingStrategy(EdxlMessage edxlMessage) {
        return switch (ConversionUtils.determineRoutingType(edxlMessage)) {
            case SAMU_TO_SAMU -> hubSanteInternalStrategy;
            case SAMU_TO_CISU -> hubToFireStrategy;
            case CISU_TO_SAMU -> fireToHubStrategy;
        };
    }

    /** Publishes a routing DTO to its exchange and routing key. */
    public void sendMessage(MessageRoutingDTO routingDTO) {
        rabbitTemplate.send(
                routingDTO.destinationExchange(), routingDTO.routingKey(), routingDTO.message());
    }

    public void initReturnsCallback() {

        // set returns callback to track undistributed messages
        rabbitTemplate.setReturnsCallback(
                returned -> {
                    // runs on a broker connection thread, which has no context of the listener
                    MessageLogContext logContext = MessageLogContext.open(returned.getMessage());
                    try {
                        handleReturnedMessage(returned);
                    } finally {
                        logContext.restore();
                    }
                });
    }

    private void handleReturnedMessage(ReturnedMessage returned) {
        EdxlMessage returnedEdxlMessage = null;
        String returnedEdxlString =
                new String(returned.getMessage().getBody(), StandardCharsets.UTF_8);
        String senderRoutingKey = getOriginalRoutingKey(returned.getMessage());

        try {
            returnedEdxlMessage =
                    isXML(returned)
                            ? edxlHandler.deserializeXmlEDXL(returnedEdxlString)
                            : edxlHandler.deserializeJsonEDXL(returnedEdxlString);
            observabilityService.bind(returnedEdxlMessage);
        } catch (JsonProcessingException e) {
            // This should never happen as if we've reached this point, the message has
            // already been deserialized
            MessageLogContext.put(
                    LogConstants.DISTRIBUTION_ID, extractDistributionId(returnedEdxlString));
            structuredLog.error(
                    String.format(
                            "Could not deserialize returned message with hashed value %s",
                            hashBody(returned.getMessage())),
                    e);
            structuredLog.debug(
                    String.format("Returned message String was %s", returnedEdxlString));
        }

        String distributionId =
                returnedEdxlMessage != null
                        ? returnedEdxlMessage.getDistributionID()
                        : DISTRIBUTION_ID_UNAVAILABLE;

        Error error = new Error();
        error.setErrorCode(ErrorCode.UNROUTABLE_MESSAGE);
        error.setErrorCause(
                "unable do deliver message to "
                        + returned.getRoutingKey()
                        + ", cause was "
                        + returned.getReplyText()
                        + " ("
                        + returned.getReplyCode()
                        + ")");
        try {
            if (isJSON(returned.getMessage())) {
                error.setSourceMessage(
                        jsonMapper.readValue(returned.getMessage().getBody(), HashMap.class));
            } else if (isXML(returned.getMessage())) {
                error.setSourceMessage(
                        xmlMapper.readValue(returned.getMessage().getBody(), HashMap.class));
            }
        } catch (IOException e) {
            structuredLog.error("Could not read message body", e);
        }
        error.setReferencedDistributionID(distributionId);
        messageHandler.logErrorMessage(error, senderRoutingKey);
        // currently, we do not handle error messages on other hubex
        if (senderRoutingKey.startsWith(FR_HEALTH_PREFIX)) {
            messageHandler.sendErrorReport(error, senderRoutingKey);
        }
        messageHandler.publishErrorMetric(
                ErrorCode.UNROUTABLE_MESSAGE.getStatusString(), senderRoutingKey);
    }

    @RabbitListener(queues = DISPATCH_QUEUE_NAME)
    @Timed(
            value = METRIC_MESSAGE_PROCESSING,
            description = "Time taken to fully dispatch a message",
            histogram = true,
            extraTags = {"operation", "dispatch"})
    public void dispatch(Message message) {
        setOriginalRoutingKeyHeader(message);
        MessageLogContext logContext = MessageLogContext.open(message);
        try {
            // Deserialize the message according to its content type
            EdxlMessage edxlMessage = messageHandler.extractMessage(message);

            RoutingStrategy strategy = selectRoutingStrategy(edxlMessage);
            // check the message is allowed to be routed (class name, inhibition, health actor
            // involvement, sender consistency, delivery mode, distributionId format)
            strategy.checkMessageContent(message, edxlMessage);
            // build the list of AMQP messages to publish, with or without conversion
            List<MessageRoutingDTO> routingDTOs =
                    strategy.buildMessageRoutingDTO(message, edxlMessage);

            for (MessageRoutingDTO routingDTO : routingDTOs) {
                sendMessage(routingDTO);
                logMessageSent(routingDTO, edxlMessage);
                if (!isConversionRouting(routingDTO)) {
                    messageHandler.publishMetrics(edxlMessage, message);
                }
            }
        } catch (AbstractHubException e) {
            messageHandler.handleError(e, message);
        } catch (Exception e) {
            // still log.error because it is not one of our AbstractHubExceptions, so there must be
            // a hole in our error cover
            structuredLog.error("Unexpected error occurred while dispatching message", e);
            throw new AmqpRejectAndDontRequeueException(e);
        } finally {
            logContext.restore();
        }
    }

    /** A converted message is published to a transfer exchange, never to {@code DISTRIBUTION_EXCHANGE}. */
    private boolean isConversionRouting(MessageRoutingDTO routingDTO) {
        return !DISTRIBUTION_EXCHANGE.equals(routingDTO.destinationExchange());
    }

    private void logMessageSent(MessageRoutingDTO routingDTO, EdxlMessage edxlMessage) {
        structuredLog.info(
                String.format(
                        "Received message (%s) has been sent to exchange (%s) with routing key (%s)",
                        edxlMessage.getDistributionID(),
                        routingDTO.destinationExchange(),
                        routingDTO.routingKey()));
    }

    @RabbitListener(queues = DISPATCH_DLQ_NAME)
    @Timed(
            value = METRIC_MESSAGE_PROCESSING,
            description = "Time taken to fully dispatch a dead letter queued message",
            histogram = true,
            extraTags = {"operation", "dispatch_dlq"})
    public void dispatchDLQ(Message message) {
        MessageLogContext logContext = MessageLogContext.open(message);
        try {
            //  If an info message sent by the Hub has not been read, we do not want to loop and
            // sent the Hub an info message back. Messages built by the Hub (e.g. error reports)
            // have no original routing key header.
            String deadFromQueue = message.getMessageProperties().getHeader(ORIGINAL_ROUTING_KEY);
            if (deadFromQueue == null || deadFromQueue.endsWith(".info")) {
                return;
            }
            EdxlMessage edxlMessage = messageHandler.extractMessage(message);
            // log message & error
            String errorCause =
                    "Message "
                            + edxlMessage.getDistributionID()
                            + " has been read from dead-letter-queue; reason was "
                            + message.getMessageProperties().getHeader(DLQ_REASON);
            String messageType =
                    EdxlUtils.getUseCaseFromMessage(edxlMessage.getFirstContentMessage());
            String recipientId = MessageUtils.getRecipientID(edxlMessage);
            DeadLetteredMessageException exception =
                    new DeadLetteredMessageException(
                            errorCause, edxlMessage.getDistributionID(), recipientId, messageType);
            messageHandler.handleError(exception, message);
        } catch (Exception e) {
            // We don't want to log again the error if it has been thrown by handleError
            // We just log the unexpected errors
            if (!(e instanceof AmqpRejectAndDontRequeueException)) {
                structuredLog.warn("Unexpected error occurred while DLQ-dispatching message", e);
            }
            throw new AmqpRejectAndDontRequeueException(e);
        } finally {
            logContext.restore();
        }
    }
}
