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
import com.hubsante.hub.exception.*;
import com.hubsante.model.EdxlHandler;
import com.hubsante.model.edxl.EdxlMessage;
import com.hubsante.model.report.ErrorCode;
import com.hubsante.model.report.ErrorReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

import static com.hubsante.hub.config.AmqpConfiguration.*;
import static com.hubsante.hub.utils.MessageUtils.*;

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

    public Dispatcher(MessageHandler messageHandler, RabbitTemplate rabbitTemplate, EdxlHandler edxlHandler) {
        this.messageHandler = messageHandler;
        this.rabbitTemplate = rabbitTemplate;
        this.edxlHandler = edxlHandler;
        initReturnsCallback();
    }

    public void initReturnsCallback() {

        // set returns callback to track undistributed messages
        rabbitTemplate.setReturnsCallback(returned -> {
            EdxlMessage returnedEdxlMessage = null;
            String returnedEdxlString = new String(returned.getMessage().getBody(), StandardCharsets.UTF_8);

            try {
                returnedEdxlMessage = edxlHandler.deserializeJsonEDXL(returnedEdxlString);
            } catch ( JsonProcessingException e) {
                // This should never happen as if we've reached this point, the message has already been deserialized
                log.error("Could not deserialize message " + returnedEdxlString, e);
            }
            ErrorReport errorReport = new ErrorReport(
                    ErrorCode.UNROUTABLE_MESSAGE,
                    "unable do deliver message to " + returned.getRoutingKey() + ", cause was " + returned.getReplyText() + " (" + returned.getReplyCode() + ")",
                    new String(returned.getMessage().getBody()),
                    returnedEdxlMessage != null ? returnedEdxlMessage.getDistributionID() : null);

            String senderRoutingKey = returned.getMessage().getMessageProperties().getHeader(DLQ_ORIGINAL_ROUTING_KEY);
            messageHandler.logErrorAndSendReport(errorReport, senderRoutingKey);
        });
    }

    @RabbitListener(queues = DISPATCH_QUEUE_NAME)
    public void dispatch(Message message) {
        try {
            // Deserialize the message according to its content type
            EdxlMessage edxlMessage = messageHandler.deserializeMessage(message);
            // Reject the message if the sender is not consistent with the routing key
            checkSenderConsistency(message, edxlMessage);
            // Reject the message if the delivery mode is not PERSISTENT
            checkDeliveryModeIsPersistent(message, edxlMessage.getDistributionID());
            // Reject the message if distributionID does not respect the format (senderID_internalID)
            checkDistributionIDFormat(edxlMessage);
            // Forward the message according to the recipient preferences. Conversion JSON <-> XML can happen here
            Message forwardedMsg = messageHandler.forwardedMessage(edxlMessage, message);
            // Extract recipient queue name from the message (explicit address and distribution kind)
            String queueName = getRecipientQueueName(edxlMessage);
            // publish the message to the recipient queue
            rabbitTemplate.send(DISTRIBUTION_EXCHANGE, queueName, forwardedMsg);
        } catch (AbstractHubException e) {
            messageHandler.handleError(e, message);
        } catch (Exception e) {
            log.error("Unexpected error occurred while dispatching message from " + message.getMessageProperties().getReceivedRoutingKey(), e);
            throw new AmqpRejectAndDontRequeueException(e);
        }
    }

    // Verifies that the distributionID respects the format senderID_internalID (e.g. fr.health.samu1234_5678)
    private void checkDistributionIDFormat(EdxlMessage message) {
        String distributionId = message.getDistributionID();
        // We  verify that senderID in the distributionID is the same as the senderID in the message
        String senderId = message.getSenderID();
        String distributionIdSenderId = distributionId.split("_")[0];
        if (!distributionIdSenderId.equals(senderId)) {
            String errorCause = "Message " + distributionId + " has been sent with an invalid distributionID format.\n" +
                    "The senderID in the distributionID should be the same as the senderID in the message.\n" +
                    "SenderID in the message: " + senderId + ", senderID in the distributionID: " + distributionIdSenderId +"\n";
            throw new InvalidDistributionIDException(errorCause, distributionId);
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
            EdxlMessage edxlMessage = messageHandler.deserializeMessage(message);
            // log message & error
            String errorCause = "Message " + edxlMessage.getDistributionID() + " has been read from dead-letter-queue; reason was " +
                    message.getMessageProperties().getHeader(DLQ_REASON);
            DeadLetteredMessageException exception = new DeadLetteredMessageException(errorCause, edxlMessage.getDistributionID());
            messageHandler.handleError(exception, message);
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




}
