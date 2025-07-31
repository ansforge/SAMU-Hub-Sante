/**
 * Copyright © 2023-2025 Agence du Numerique en Sante (ANS)
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
import com.hubsante.hub.config.Constants;
import com.hubsante.hub.config.HubConfiguration;
import com.hubsante.hub.exception.*;
import com.hubsante.hub.utils.ConversionRulesCommand;
import com.hubsante.hub.utils.ConversionUtils;
import com.hubsante.hub.utils.EdxlUtils;
import com.hubsante.hub.utils.MessageUtils;
import com.hubsante.model.EdxlHandler;
import com.hubsante.model.edxl.EdxlMessage;
import com.hubsante.model.report.ErrorCode;
import com.hubsante.model.report.Error;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;

import static com.hubsante.hub.config.AmqpConfiguration.*;
import static com.hubsante.hub.utils.MessageUtils.*;
import static com.hubsante.hub.config.Constants.*;

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
    private final ConversionHandler conversionHandler;

    private final String SAMU_070_CLIENT_ID = "fr.health.samu070";

    public Dispatcher(MessageHandler messageHandler, RabbitTemplate rabbitTemplate, EdxlHandler edxlHandler, XmlMapper xmlMapper, ObjectMapper jsonMapper, ConversionHandler conversionHandler) {
        this.messageHandler = messageHandler;
        this.rabbitTemplate = rabbitTemplate;
        this.edxlHandler = edxlHandler;
        this.xmlMapper = xmlMapper;
        this.jsonMapper = jsonMapper;
        this.conversionHandler = conversionHandler;
        initReturnsCallback();
    }

    public void initReturnsCallback() {

        // set returns callback to track undistributed messages
        rabbitTemplate.setReturnsCallback(returned -> {
            EdxlMessage returnedEdxlMessage = null;
            String returnedEdxlString = new String(returned.getMessage().getBody(), StandardCharsets.UTF_8);

            try {
                returnedEdxlMessage = isXML(returned) ?
                        edxlHandler.deserializeXmlEDXL(returnedEdxlString) :
                        edxlHandler.deserializeJsonEDXL(returnedEdxlString);
            } catch ( JsonProcessingException e) {
                // This should never happen as if we've reached this point, the message has already been deserialized
                log.error("Could not deserialize message " + returnedEdxlString, e);
            }
            Error error = new Error();
            error.setErrorCode(ErrorCode.UNROUTABLE_MESSAGE);
            error.setErrorCause("unable do deliver message to " + returned.getRoutingKey() + ", cause was " + returned.getReplyText() + " (" + returned.getReplyCode() + ")");
            try {
                if (isJSON(returned.getMessage())) {
                    error.setSourceMessage(jsonMapper.readValue(returned.getMessage().getBody(), HashMap.class));
                } else if (isXML(returned.getMessage())) {
                    error.setSourceMessage(xmlMapper.readValue(returned.getMessage().getBody(), HashMap.class));
                }
            } catch (IOException e) {
                log.error("Could not read message body", e);
            }
            error.setReferencedDistributionID(returnedEdxlMessage != null ? returnedEdxlMessage.getDistributionID() : DISTRIBUTION_ID_UNAVAILABLE);
            String senderRoutingKey = returned.getMessage().getMessageProperties().getHeader(ORIGINAL_ROUTING_KEY);
            // currently, we do not handle error messages on other hubex
            if (senderRoutingKey.startsWith(FR_HEALTH_PREFIX)) {
                messageHandler.logErrorAndSendReport(error, senderRoutingKey);
            }
            messageHandler.publishErrorMetric(ErrorCode.UNROUTABLE_MESSAGE.getStatusString(), senderRoutingKey);
        });
    }

    @RabbitListener(queues = DISPATCH_QUEUE_NAME)
    @Timed(value = DISPATCH_TIMED_METRIC, description = "Time taken to fully dispatch a message")
    public void dispatch(Message message) {
        try {
            setOriginalRoutingKeyHeader(message);
            // Deserialize the message according to its content type
            EdxlMessage edxlMessage = messageHandler.extractMessage(message);
            // reject the message if no health actor is involved (as sender or recipient)
            checkHealthActorIsInvolved(edxlMessage);
            // ToDo: see how hubConfig should be made available to the Dispatcher (and remove getter in MessageHandler)
            // ToDo: check this only on specific vhosts (like 15-NexSIS)?
            // Reject the message if the sender is not consistent with the routing key
            checkSenderConsistency(message, edxlMessage);
            // Reject the message if the delivery mode is not PERSISTENT
            checkDeliveryModeIsPersistent(message, edxlMessage.getDistributionID());
            // Reject the message if distributionID does not respect the format (senderID_internalID)
            if (message.getMessageProperties().getReceivedRoutingKey().startsWith(Constants.FR_HEALTH_PREFIX)) {
                checkDistributionIDFormat(edxlMessage);
            }

            // VERRUE POUR SAMU-070
            if (message.getMessageProperties().getReceivedRoutingKey().equals(SAMU_070_CLIENT_ID) || edxlMessage.getSenderID().equals(SAMU_070_CLIENT_ID)) {
                boolean isConversionRequired = ConversionUtils.requiresConversion(messageHandler.getHubConfig(), edxlMessage);
                // Forward the message according to the recipient preferences. Conversion JSON <-> XML can happen here
                Message forwardedMsg = messageHandler.forwardedMessage(edxlMessage, message);
                if (isConversionRequired) {
                    ConversionRulesCommand conversionRulesCommand = new ConversionRulesCommand(edxlMessage, messageHandler);
                    String convertedMessage = conversionHandler.applyConversionRules(conversionRulesCommand);
                    forwardedMsg = messageHandler.forwardedStringMessage(convertedMessage, message);
                }
                // Extract recipient queue name from the message (explicit address and distribution kind)
                String queueName = getRecipientQueueName(edxlMessage);
                // publish the message to the recipient queue
                rabbitTemplate.send(DISTRIBUTION_EXCHANGE, queueName, forwardedMsg);
                messageHandler.publishMetrics(edxlMessage, forwardedMsg);
                return;
            }

            // check message type is allowed on the current vhost
            checkMessageClassNameSupported(edxlMessage, messageHandler.getHubConfig());

            boolean isConversionRequired = ConversionUtils.requiresConversion(messageHandler.getHubConfig(), edxlMessage);
            if (isConversionRequired) {
                ConversionRulesCommand conversionRulesCommand = new ConversionRulesCommand(edxlMessage, messageHandler);
                String convertedMessage = conversionHandler.applyConversionRules(conversionRulesCommand);
                sendToTransferExchange(convertedMessage, message, conversionRulesCommand);
                log.debug("The converted message has been sent to the exchange to reach the recipient's vhost.");
                // We MUST return here to exit the dispatch() function, otherwise the message will be published on the source Exchange as well
                return;
            }

            // Forward the message according to the recipient preferences. Conversion JSON <-> XML can happen here
            Message forwardedMsg = messageHandler.forwardedMessage(edxlMessage, message);
            // Extract recipient queue name from the message (explicit address and distribution kind)
            String queueName = getRecipientQueueName(edxlMessage);
            // publish the message to the recipient queue
            rabbitTemplate.send(DISTRIBUTION_EXCHANGE, queueName, forwardedMsg);
            messageHandler.publishMetrics(edxlMessage, forwardedMsg);
        } catch (AbstractHubException e) {
            messageHandler.handleError(e, message);
        } catch (Exception e) {
            // still log.error because it is not one of our AbstractHubExceptions, so there must be
            // a hole in our error cover
            log.error("Unexpected error occurred while dispatching message from " + message.getMessageProperties().getReceivedRoutingKey(), e);
            throw new AmqpRejectAndDontRequeueException(e);
        }
    }

    public void sendToTransferExchange(String convertedMessage, Message message, ConversionRulesCommand conversionRulesCommand){
        Message forwardedMsg = messageHandler.forwardedStringMessage(convertedMessage, message);

        String transferExchangeName = ConversionUtils.buildExchangeDestination(conversionRulesCommand.getSourceVHost(), conversionRulesCommand.getTargetVHost());

        String routingKey = message.getMessageProperties().getReceivedRoutingKey();

        log.info("Message transferred to exchange: {} with routing key: {}", transferExchangeName, routingKey);

        rabbitTemplate.send(transferExchangeName, routingKey, forwardedMsg);
    }

    @RabbitListener(queues = DISPATCH_DLQ_NAME)
    @Timed(value = DLQ_TIMED_METRIC, description = "Time taken to fully dispatch a dead letter queued message")
    public void dispatchDLQ(Message message) {
        try {
            //  If an info message sent by the Hub has not been read, we do not want to loop and sent the Hub an info message back
            String deadFromQueue = message.getMessageProperties().getHeader(ORIGINAL_ROUTING_KEY);
            if (deadFromQueue.endsWith(".info")) {
                return;
            }
            EdxlMessage edxlMessage = messageHandler.extractMessage(message);
            // log message & error
            String errorCause = "Message " + edxlMessage.getDistributionID() + " has been read from dead-letter-queue; reason was " +
                    message.getMessageProperties().getHeader(DLQ_REASON);
            DeadLetteredMessageException exception = new DeadLetteredMessageException(errorCause, edxlMessage.getDistributionID());
            messageHandler.handleError(exception, message);
        } catch (Exception e) {
            // We don't want to log again the error if it has been thrown by handleError
            // We just log the unexpected errors
            if (!(e instanceof AmqpRejectAndDontRequeueException)) {
                String originalRoutingKey = message.getMessageProperties().getHeader(ORIGINAL_ROUTING_KEY) != null ?
                        message.getMessageProperties().getHeader(ORIGINAL_ROUTING_KEY) : "Unknown routing key";
                log.warn("Unexpected error occurred while DLQ-dispatching message from " + originalRoutingKey, e);
            }
            throw new AmqpRejectAndDontRequeueException(e);
        }
    }
}
