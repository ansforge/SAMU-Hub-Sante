package com.hubsante.dispatcher.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hubsante.hub.service.EdxlHandler;
import com.hubsante.model.edxl.EdxlMessage;
import com.hubsante.model.report.ErrorReport;
import org.apache.commons.compress.utils.FileNameUtils;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.OffsetDateTime;

import static com.hubsante.hub.config.AmqpConfiguration.*;

public class MessageTestUtils {

    static ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    public static Message createMessage(String filename, String contentType, String receivedRoutingKey) throws IOException {
        File edxlFile = new File(classLoader.getResource("messages/" + filename).getFile());

        String edxlString = Files.readString(edxlFile.toPath());

        MessageProperties properties = new MessageProperties();
        properties.setReceivedRoutingKey(receivedRoutingKey);
        // Spring AMQP uses receivedDeliveryMode on consumers, and deliveryMode on producers
        // When testing consumers (aka the SpringAMPQ Message passed as a method parameter),
        // we need to set the receivedDeliveryMode, which is PERSISTENT by default
        //
        // When testing producers we would need to set the deliveryMode only to test the CheckDeliveryMode method
        // (already tested in DispatcherTest)
        properties.setReceivedDeliveryMode(MessageDeliveryMode.PERSISTENT);

        Message createdMessage = new Message(edxlString.getBytes(StandardCharsets.UTF_8), properties);
        createdMessage.getMessageProperties().setContentType(contentType);

        return createdMessage;
    }

    public static Message createMessage(String filename, String receivedRoutingKey) throws IOException {
        return createMessage(filename, getContentTypeFromFilename(filename), receivedRoutingKey);
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

    public static void setCustomExpirationDate(EdxlMessage edxlMessage, long offset) {
        OffsetDateTime now = OffsetDateTime.now();
        edxlMessage.setDateTimeSent(now);
        edxlMessage.setDateTimeExpires(now.plusNanos(offset));
    }
}
