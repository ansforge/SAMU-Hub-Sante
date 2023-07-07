package com.hubsante.dispatcher.utils;

import com.hubsante.hub.service.EdxlHandler;
import com.hubsante.model.edxl.EdxlMessage;
import org.apache.commons.compress.utils.FileNameUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePropertiesBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class MessageTestUtils {

    static ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    static EdxlHandler edxlHandler = new EdxlHandler();

    public static Message createMessage(String filename, String contentType, String receivedRoutingKey) throws IOException {
        File edxlFile = new File(classLoader.getResource("messages/" + filename).getFile());

        String edxlString = Files.readString(edxlFile.toPath());

        MessagePropertiesBuilder builder = MessagePropertiesBuilder.newInstance();
        if (contentType != null) {
            builder.setContentType(contentType);
        }
        MessageProperties properties = builder.setReceivedRoutingKey(receivedRoutingKey).build();

        return new Message(edxlString.getBytes(StandardCharsets.UTF_8), properties);
    }

    public static Message createMessage(String filename, String receivedRoutingKey) throws IOException {
        return createMessage(filename, getContentTypeFromFilename(filename), receivedRoutingKey);
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
}
