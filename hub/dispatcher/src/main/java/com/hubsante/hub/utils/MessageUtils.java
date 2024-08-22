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
package com.hubsante.hub.utils;

import com.hubsante.hub.exception.DeliveryModeInconsistencyException;
import com.hubsante.hub.exception.ExpiredBeforeDispatchMessageException;
import com.hubsante.hub.exception.InvalidDistributionIDException;
import com.hubsante.hub.exception.SenderInconsistencyException;
import com.hubsante.modelsinterface.edxl.DistributionKind;
import com.hubsante.modelsinterface.interfaces.EdxlMessageInterface;
import com.hubsante.modelsinterface.interfaces.EdxlServiceInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class MessageUtils {
    @Autowired
    private EdxlServiceInterface edxlService;
    
    private static final String HEALTH_PREFIX = "fr.health";
    public static String getSenderFromRoutingKey(Message message) {
        return message.getMessageProperties().getReceivedRoutingKey();
    }
    public static void checkSenderConsistency(Message message, EdxlMessageInterface edxlMessage) {
        String receivedRoutingKey = getSenderFromRoutingKey(message);
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

    public static void checkDeliveryModeIsPersistent(Message message, String messageId) {
        if (!MessageDeliveryMode.PERSISTENT.equals(message.getMessageProperties().getReceivedDeliveryMode())) {
            String errorCause = "Message " + messageId + " has been sent with non-persistent delivery mode";
            throw new DeliveryModeInconsistencyException(errorCause, messageId);
        }
    }

    public static String getInfoQueueNameFromClientId(String clientId) {
        return clientId + ".info";
    }

    public String getRecipientID(EdxlMessageInterface edxlMessage) {
        return edxlService.getDescriptorExplicitAddressValue(edxlMessage);
    }

    public String getRecipientQueueName(EdxlMessageInterface edxlMessage) {
        return getRecipientID(edxlMessage) + "." + getQueueType(edxlMessage.getDistributionKind());
    }

    public static String getQueueType(DistributionKind kind) {
        switch (kind) {
            case ACK:
                return "ack";
            case ERROR:
                return "info";
            default:
                return "message";
        }
    }

    public static boolean convertToXML(String recipientID, Boolean useXML) {
        // sending message to outer hubex is always XML
        if (!recipientID.startsWith(HEALTH_PREFIX)) {
            return true;
        }
        // sending message to health clients is based on client preference (default to JSON)
        return useXML != null
                && useXML;
    }

    public static boolean isJSON(Message message) {
        return MessageProperties.CONTENT_TYPE_JSON.equals(message.getMessageProperties().getContentType());
    }

    public static boolean isXML(Message message) {
        return MessageProperties.CONTENT_TYPE_XML.equals(message.getMessageProperties().getContentType());
    }

    public static void overrideExpirationIfNeeded(EdxlMessageInterface edxlMessage, MessageProperties properties, long defaultTTL) {
        // OffsetDateTime comes with seconds and nanos, not millis
        // We assume that one second is an acceptable interval
        long queueExpirationDateTime = OffsetDateTime.now().plusSeconds(defaultTTL).toEpochSecond();
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

    // Verifies that the distributionID respects the format senderID_internalID (e.g. fr.health.samu1234_5678)
    public static void checkDistributionIDFormat(EdxlMessageInterface message) {
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
}
