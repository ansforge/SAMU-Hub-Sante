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
package com.hubsante.hub.utils;

import com.hubsante.hub.config.HubConfiguration;
import com.hubsante.hub.config.LogConstants;
import com.hubsante.hub.config.StructuredLogger;
import com.hubsante.hub.exception.*;
import com.hubsante.model.edxl.DistributionKind;
import com.hubsante.model.edxl.EdxlMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.hubsante.hub.config.AmqpConfiguration.ORIGINAL_ROUTING_KEY;
import static com.hubsante.hub.config.Constants.DISTRIBUTION_ID_UNAVAILABLE;

@Slf4j
public class MessageUtils {
    private final static StructuredLogger structuredLog = new StructuredLogger(log);

    static final String HEALTH_PREFIX = "fr.health";
    private static final String CISU_LRM_USER = "fr.cisu.sdisY";

    private static final String SDISZ_LRM_USER = "fr.fire.nexsis.sdisZ";
    public static String getSenderFromRoutingKey(Message message) {
        String receivedRoutingKey = message.getMessageProperties().getReceivedRoutingKey();
        return receivedRoutingKey != null ? receivedRoutingKey : "";
    }

    public static void setOriginalRoutingKeyHeader(Message message) {
        // we need to store the original routing key to keep track of the original sender, even after generating a new routing key based on recipient & distributionKind
        message.getMessageProperties().setHeader(ORIGINAL_ROUTING_KEY, getSenderFromRoutingKey(message));
    }

    public static void checkSenderConsistency(Message message, EdxlMessage edxlMessage) {
        String receivedRoutingKey = getSenderFromRoutingKey(message);
        if (!receivedRoutingKey.equals(edxlMessage.getSenderID())) {
            if (!receivedRoutingKey.startsWith(HEALTH_PREFIX)) {
                String senderID = edxlMessage.getSenderID();
                String recipientID = getRecipientID(edxlMessage);
                String messageType = EdxlUtils.getUseCaseFromMessage(edxlMessage.getFirstContentMessage());
                structuredLog.info(
                        String.format("Message has been received from hubex partner with routing key %s and senderID %s", receivedRoutingKey, senderID),
                        Map.of(LogConstants.DISTRIBUTION_ID, edxlMessage.getDistributionID(), LogConstants.SENDER_ID, senderID, LogConstants.RECIPIENT_ID, recipientID, LogConstants.MESSAGE_TYPE, messageType)
                );
                return;
            }
            String errorCause = "Sender inconsistency for message " +
                    edxlMessage.getDistributionID() +
                    " : message sender is " +
                    edxlMessage.getSenderID() +
                    " but received routing key is " +
                    receivedRoutingKey;
            throw new SenderInconsistencyException(errorCause, edxlMessage.getDistributionID());
        }
    }

    public static void checkHealthActorIsInvolved(EdxlMessage edxlMessage) {
        if(!edxlMessage.getSenderID().startsWith(HEALTH_PREFIX) &&
                !edxlMessage.getDescriptor().getExplicitAddress().getExplicitAddressValue().startsWith(HEALTH_PREFIX)) {
            String errorCause = "Unable to route message with id " + edxlMessage.getDistributionID() + ", no health actor involved.";
            throw new UnroutableMessageException(errorCause, edxlMessage.getDistributionID());
        }
    }

    public static void checkDeliveryModeIsPersistent(Message message, String distributionId) {
        if (!MessageDeliveryMode.PERSISTENT.equals(message.getMessageProperties().getReceivedDeliveryMode())) {
            if (!message.getMessageProperties().getReceivedRoutingKey().startsWith(HEALTH_PREFIX)) {
                String senderID = getSenderFromRoutingKey(message);
                structuredLog.error(
                        String.format("Message has been received from hubex with routing key %s without persistent mode enabled", senderID),
                        Map.of(LogConstants.SENDER_ID, senderID, LogConstants.DISTRIBUTION_ID, distributionId)
                );
                return;
            }
            String errorCause = "Message " + distributionId + " has been sent with non-persistent delivery mode";
            throw new DeliveryModeInconsistencyException(errorCause, distributionId);
        }
    }


    public static void checkMessageClassNameSupported(EdxlMessage edxlMessage, HubConfiguration hubConfig) throws Exception {
        String messageClassName = EdxlUtils.getUseCaseFromMessage(edxlMessage.getFirstContentMessage());
        List<String> supportedMessages = hubConfig.getSupportedMessages();

        boolean isMessageClassNameSupported = supportedMessages.contains(messageClassName);
        if(!isMessageClassNameSupported){
            throw new UnroutableMessageException("The received message classname is not supported on this vhost", edxlMessage.getDistributionID());
        }
    }

    public static String getInfoQueueNameFromClientId(String clientId) {
        return clientId + ".info";
    }

    public static String getRecipientID(EdxlMessage edxlMessage) {
        return edxlMessage.getDescriptor().getExplicitAddress().getExplicitAddressValue();
    }

    public static String getRecipientQueueName(EdxlMessage edxlMessage) {
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
        if (!(recipientID.startsWith(HEALTH_PREFIX) || recipientID.equals(CISU_LRM_USER) || recipientID.equals(SDISZ_LRM_USER))) {
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
        return (MessageProperties.CONTENT_TYPE_XML.equals(message.getMessageProperties().getContentType()) ||
                message.getMessageProperties().getReceivedRoutingKey() == null ||
                !message.getMessageProperties().getReceivedRoutingKey().startsWith(HEALTH_PREFIX));
    }

    public static boolean isXML(ReturnedMessage returned) {
        return MessageProperties.CONTENT_TYPE_XML.equals(returned.getMessage().getMessageProperties().getContentType()) ||
                        !returned.getRoutingKey().startsWith(HEALTH_PREFIX);
    }

    public static void overrideExpirationIfNeeded(EdxlMessage edxlMessage, MessageProperties properties, long defaultTTL) {
        // OffsetDateTime comes with seconds and nanos, not millis
        // We assume that one second is an acceptable interval
        long messageCustomExpirationDateTime = edxlMessage.getDateTimeExpires().toEpochSecond();
        String distributionId = edxlMessage.getDistributionID();
        String senderID = edxlMessage.getSenderID();
        String recipientID = getRecipientID(edxlMessage);
        String messageType = EdxlUtils.getUseCaseFromMessage(edxlMessage.getFirstContentMessage());

        // Reset the expiration header if it was set by the client,
        // they should use dateTimeExpires for that purpose.
        if (Objects.nonNull(properties.getExpiration())) {
            structuredLog.info(
                    String.format("Reset expiration header for message %s that was originally set to %s", distributionId, properties.getExpiration()),
                    Map.of(LogConstants.DISTRIBUTION_ID, distributionId, LogConstants.SENDER_ID, senderID, LogConstants.RECIPIENT_ID, recipientID, LogConstants.MESSAGE_TYPE, messageType)
            );
            properties.setExpiration(null);
        }

        // if edxl.dateTimeExpires is in the past
        // we throw an ExpiredBeforeDispatchMessageException error
        long newTTL = messageCustomExpirationDateTime - OffsetDateTime.now().toEpochSecond();
        boolean isNewTTLInThePast =  newTTL <= 0;
        if (isNewTTLInThePast) {
            String errorCause = "Message " + edxlMessage.getDistributionID() + " has expired before reaching the recipient queue";
            throw new ExpiredBeforeDispatchMessageException(errorCause, edxlMessage.getDistributionID());
        }

        // if edxl.dateTimeExpires is before default expiration time (now + default queue TTl),
        // we have to override expiration header in AMQP message
        long defaultExpirationDateTime = OffsetDateTime.now().plusSeconds(defaultTTL).toEpochSecond();
        if (messageCustomExpirationDateTime < defaultExpirationDateTime) {
            properties.setExpiration(String.valueOf(newTTL * 1000));
            String dateTimeExpires = edxlMessage.getDateTimeExpires().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            structuredLog.info(
                    String.format("override expiration for message %s: expiration is now %s", distributionId, dateTimeExpires),
                    Map.of(LogConstants.DISTRIBUTION_ID, distributionId, LogConstants.SENDER_ID, senderID, LogConstants.RECIPIENT_ID, recipientID, LogConstants.MESSAGE_TYPE, messageType)
            );
        }
    }

    // Verifies that the distributionID respects the format senderID_internalID (e.g. fr.health.samu1234_5678)
    public static void checkDistributionIDFormat(EdxlMessage message) {
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

    public static String extractDistributionId(String edxlString) {
        // Regex pattern to match the distributionID value
        String jsonRegex = "\"distributionID\"\\s*:\\s*\"([\\w._-]+)\"";
        Pattern jsonPattern = Pattern.compile(jsonRegex);
        Matcher jsonMatcher = jsonPattern.matcher(edxlString);

        String xmlRegex = "<distributionID>([\\w._-]+)</distributionID>";
        Pattern xmlPattern = Pattern.compile(xmlRegex);
        Matcher xmlMatcher = xmlPattern.matcher(edxlString);

        if (jsonMatcher.find()) {
            return jsonMatcher.group(1);
        } else if(xmlMatcher.find()) {
            return xmlMatcher.group(1);
        } else {
            return DISTRIBUTION_ID_UNAVAILABLE;
        }
    }

    public static String hashBody(Message message){
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");byte[] hashedBytes = digest.digest(body.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            String senderID = getSenderFromRoutingKey(message);
            structuredLog.error(
                    "Could not get SHA-256 algorithm",
                    Map.of(LogConstants.SENDER_ID, senderID)
            );
            throw new RuntimeException(e);
        }

    }
}
