package com.hubsante.hub.service.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hubsante.model.EdxlHandler;
import com.hubsante.model.TestMessagesHelper;
import com.hubsante.model.edxl.EdxlMessage;
import com.hubsante.model.report.ErrorReport;
import org.apache.commons.compress.utils.FileNameUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

import static com.hubsante.hub.config.AmqpConfiguration.*;

public class MessageTestUtils {
    public static Message createMessage(String filename, String contentType, String receivedRoutingKey) throws IOException {
        boolean isXML = MessageProperties.CONTENT_TYPE_XML.equals(contentType);
        String edxlString = TestMessagesHelper.getSampleMessageAsStream(filename, isXML);

        MessageProperties properties = getMessageProperties(receivedRoutingKey);

        Message createdMessage = new Message(edxlString.getBytes(StandardCharsets.UTF_8), properties);
        createdMessage.getMessageProperties().setContentType(contentType);

        return createdMessage;
    }

    public static Message createMessage(String filename, String receivedRoutingKey) throws IOException {
        return createMessage(filename, getContentTypeFromFilename(filename), receivedRoutingKey);
    }

    public static Message createInvalidMessage(String filename, String receivedRoutingKey) throws IOException {
        return createInvalidMessage(filename, getContentTypeFromFilename(filename), receivedRoutingKey);
    }

    public static Message createInvalidMessage(String filename, String contentType, String receivedRoutingKey) throws IOException {
        String edxlString = TestMessagesHelper.getInvalidMessageAsStream(filename);

        MessageProperties properties = getMessageProperties(receivedRoutingKey);

        Message createdMessage = new Message(edxlString.getBytes(StandardCharsets.UTF_8), properties);
        createdMessage.getMessageProperties().setContentType(contentType);

        return createdMessage;
    }

    @NotNull
    private static MessageProperties getMessageProperties(String receivedRoutingKey) {
        MessageProperties properties = new MessageProperties();
        properties.setReceivedRoutingKey(receivedRoutingKey);
        // Spring AMQP uses receivedDeliveryMode on consumers, and deliveryMode on producers
        // When testing consumers (aka the SpringAMPQ Message passed as a method parameter),
        // we need to set the receivedDeliveryMode, which is PERSISTENT by default
        //
        // When testing producers we would need to set the deliveryMode only to test the CheckDeliveryMode method
        // (already tested in DispatcherTest)
        properties.setReceivedDeliveryMode(MessageDeliveryMode.PERSISTENT);
        return properties;
    }

    public static Message applyRabbitmqDLQHeaders(Message originalMessage, String dlqReason) {
        originalMessage.getMessageProperties().setHeader(DLQ_REASON, dlqReason);
        originalMessage.getMessageProperties().setHeader(DLQ_ORIGINAL_ROUTING_KEY,
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

    public static ErrorReport getErrorReportFromMessage(EdxlHandler edxlHandler, Message message) throws JsonProcessingException {

        String msgString = new String(message.getBody());

        return message.getMessageProperties().getContentType().equals(MessageProperties.CONTENT_TYPE_XML) ?
                (ErrorReport) edxlHandler.deserializeXmlContentMessage(msgString) :
                (ErrorReport) edxlHandler.deserializeJsonContentMessage(msgString);
    }

    public static void setCustomExpirationDate(EdxlMessage edxlMessage, long offset_in_seconds) {
        OffsetDateTime now = OffsetDateTime.now();
        edxlMessage.setDateTimeSent(now);
        edxlMessage.setDateTimeExpires(now.plusSeconds(offset_in_seconds));
    }
}
