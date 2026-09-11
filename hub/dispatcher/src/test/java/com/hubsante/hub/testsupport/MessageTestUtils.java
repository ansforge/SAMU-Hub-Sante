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
package com.hubsante.hub.testsupport;

import static com.hubsante.hub.config.AmqpConfiguration.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hubsante.model.EdxlHandler;
import com.hubsante.model.edxl.EdxlMessage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import org.apache.commons.compress.utils.FileNameUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.testcontainers.shaded.com.google.common.io.ByteStreams;

public class MessageTestUtils {

    private static final EdxlHandler edxlHandler = new EdxlHandler();

    public static String getSampleMessage(String message, boolean isXML) throws IOException {
        String extension = isXML ? ".xml" : ".json";
        String filepath = message + "/" + message + extension;
        String json;
        try (InputStream is =
                MessageTestUtils.class
                        .getClassLoader()
                        .getResourceAsStream("sample/valid/" + filepath)) {
            assert is != null;
            json = new String(ByteStreams.toByteArray(is), StandardCharsets.UTF_8);
        }
        return json;
    }

    public static String getInvalidMessage(String messagePath) throws IOException {
        String json;
        try (InputStream is =
                MessageTestUtils.class
                        .getClassLoader()
                        .getResourceAsStream("sample/failing/" + messagePath)) {
            assert is != null;
            json = new String(ByteStreams.toByteArray(is), StandardCharsets.UTF_8);
        }
        return json;
    }

    public static Message createMessage(String filename, String contentType) throws IOException {
        return createMessage(filename, contentType, null, null);
    }

    public static Message createMessage(String filename, String contentType, String sender)
            throws IOException {
        return createMessage(filename, contentType, sender, null);
    }

    public static Message createMessage(
            String filename, String contentType, String sender, String recipient)
            throws IOException {
        boolean isXML = MessageProperties.CONTENT_TYPE_XML.equals(contentType);
        String edxlString = getSampleMessage(filename, isXML);
        EdxlMessage edxlMessage = deserialize(edxlString, isXML);
        String effectiveSender = sender != null ? sender : edxlMessage.getSenderID();

        if (sender != null) {
            String existingDistributionId = edxlMessage.getDistributionID();
            int separator = existingDistributionId.indexOf('_');
            String uuid =
                    separator >= 0
                            ? existingDistributionId.substring(separator + 1)
                            : existingDistributionId;
            edxlMessage.setSenderID(sender);
            edxlMessage.setDistributionID(sender + "_" + uuid);
        }

        if (recipient != null) {
            edxlMessage.getDescriptor().getExplicitAddress().setExplicitAddressValue(recipient);
        }

        String serialized = serialize(edxlMessage, isXML);
        byte[] body = serialized.getBytes(StandardCharsets.UTF_8);

        MessageProperties properties = buildMessageProperties(effectiveSender);
        Message message = new Message(body, properties);
        message.getMessageProperties().setContentType(contentType);
        return message;
    }

    private static EdxlMessage deserialize(String edxlString, boolean isXML)
            throws JsonProcessingException {
        return isXML
                ? edxlHandler.deserializeXmlEDXL(edxlString)
                : edxlHandler.deserializeJsonEDXL(edxlString);
    }

    private static String serialize(EdxlMessage edxlMessage, boolean isXML)
            throws JsonProcessingException {
        return isXML
                ? edxlHandler.serializeXmlEDXL(edxlMessage)
                : edxlHandler.serializeJsonEDXL(edxlMessage);
    }

    public static Message createInvalidMessage(String filename, String receivedRoutingKey)
            throws IOException {
        return createInvalidMessage(
                filename, getContentTypeFromFilename(filename), receivedRoutingKey);
    }

    public static Message createInvalidMessage(
            String filename, String contentType, String receivedRoutingKey) throws IOException {
        String edxlString = getInvalidMessage(filename);

        MessageProperties properties = buildMessageProperties(receivedRoutingKey);

        Message createdMessage =
                new Message(edxlString.getBytes(StandardCharsets.UTF_8), properties);
        createdMessage.getMessageProperties().setContentType(contentType);

        return createdMessage;
    }

    @NotNull
    private static MessageProperties buildMessageProperties(String receivedRoutingKey) {
        MessageProperties properties = new MessageProperties();
        properties.setReceivedRoutingKey(receivedRoutingKey);
        // Spring AMQP uses receivedDeliveryMode on consumers, and deliveryMode on producers
        // When testing consumers (aka the SpringAMPQ Message passed as a method parameter),
        // we need to set the receivedDeliveryMode, which is PERSISTENT by default
        //
        // When testing producers we would need to set the deliveryMode only to test the
        // CheckDeliveryMode method
        // (already tested in DispatcherTest)
        properties.setReceivedDeliveryMode(MessageDeliveryMode.PERSISTENT);
        return properties;
    }

    public static Message applyRabbitmqDLQHeaders(Message originalMessage, String dlqReason) {
        originalMessage.getMessageProperties().setHeader(DLQ_REASON, dlqReason);
        originalMessage
                .getMessageProperties()
                .setHeader(
                        ORIGINAL_ROUTING_KEY,
                        originalMessage.getMessageProperties().getReceivedRoutingKey());

        return originalMessage;
    }

    public static String getContentTypeFromFilename(String filename) {
        switch (FileNameUtils.getExtension(filename)) {
            case "json":
                return MessageProperties.CONTENT_TYPE_JSON;
            case "xml":
                return MessageProperties.CONTENT_TYPE_XML;
            default:
                return null;
        }
    }

    public static void setCustomExpirationDate(EdxlMessage edxlMessage, long offset_in_seconds) {
        OffsetDateTime now = OffsetDateTime.now();
        edxlMessage.setDateTimeSent(now);
        edxlMessage.setDateTimeExpires(now.plusSeconds(offset_in_seconds));
    }
}
