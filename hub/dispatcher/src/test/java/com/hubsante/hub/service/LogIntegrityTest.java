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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.hubsante.hub.HubApplication;
import com.hubsante.hub.config.HubConfiguration;
import com.hubsante.model.EdxlHandler;
import com.hubsante.model.Validator;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePropertiesBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.test.context.SpringRabbitTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@SpringBootTest
@ContextConfiguration(classes = HubApplication.class)
@SpringRabbitTest
public class LogIntegrityTest {

    private RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private MessagePersistenceService persistenceService = mock(MessagePersistenceService.class);
    @Autowired private EdxlHandler edxlHandler;
    @Autowired private Validator validator;
    @Autowired private HubConfiguration hubConfiguration;
    @Autowired private MeterRegistry meterRegistry;
    @Autowired private XmlMapper xmlMapper;
    @Autowired private ObjectMapper jsonMapper;
    static ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    private Dispatcher dispatcher;

    private Message amqpMessage;

    private static final String SENDER_ID = "fr.health.samuV3";
    private static final String INPUT_HASH = "9slJ9F2xZCUd/JM2WM9n5+Dk+OnkxrLaHW1CvJmsb78=";
    private static final String OUTPUT_HASH = "RBNvo1WzZ4oRRq0W9+hknpT7T8If536DEMBg9hyq/4o=";
    private static final String INPUT_MESSAGE_CONTENT_TYPE = MessageProperties.CONTENT_TYPE_JSON;

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry propertiesRegistry) {
        propertiesRegistry.add(
                "supported.messages.file",
                () ->
                        Objects.requireNonNull(
                                classLoader.getResource("config/supported.messages.csv")));
        propertiesRegistry.add(
                "client.preferences.file",
                () ->
                        Objects.requireNonNull(
                                classLoader.getResource("config/client.preferences.csv")));
        propertiesRegistry.add(
                "client.configuration.file",
                () -> Objects.requireNonNull(classLoader.getResource("config/clients.yaml"))
        );
        propertiesRegistry.add("dispatcher.default.ttl", () -> 600);
        propertiesRegistry.add("spring.rabbitmq.virtual-host", () -> "15-15_v2.1");
    }

    @PostConstruct
    void setup() throws Exception {

        // Arrange: Prepare a JSON AMQP message from input file
        MessageProperties props =
                MessagePropertiesBuilder.newInstance()
                        .setContentType(INPUT_MESSAGE_CONTENT_TYPE)
                        .build();
        props.setReceivedRoutingKey(SENDER_ID);
        props.setReceivedDeliveryMode(MessageDeliveryMode.PERSISTENT);
        byte[] body =
                this.getClass()
                        .getClassLoader()
                        .getResourceAsStream("sample/input_integrity_message")
                        .readAllBytes();
        amqpMessage = new Message(body, props);

        // Mock web client to output conversion call body in a local file
        WebClient webClient = mock(WebClient.class);
        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        // String value returned by conversion API
        String convertedJsonString =
                new String(
                        classLoader
                                .getResourceAsStream("sample/conversion-response-content.json")
                                .readAllBytes(),
                        StandardCharsets.UTF_8);
        Mono<String> responseMono = Mono.just(convertedJsonString);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.contentType(any())).thenReturn(requestBodyUriSpec);

        // Capture the bodyValue argument to write it to a file
        doAnswer(
                        invocation -> {
                            String requestBody = invocation.getArgument(0);
                            // Write requestBody to a file under at root of project
                            Path outDir = Paths.get("src", "test", "resources");
                            Files.createDirectories(outDir);
                            Path outFile = outDir.resolve("sample/conversion-request-body.json");
                            Files.writeString(outFile, requestBody);
                            // Return mocked value
                            return requestHeadersSpec;
                        })
                .when(requestBodyUriSpec)
                .bodyValue(any());

        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(responseMono);

        ConversionHandler conversionHandler = new ConversionHandler(webClient, edxlHandler);

        MessageHandler messageHandler =
                new MessageHandler(
                        rabbitTemplate,
                        edxlHandler,
                        hubConfiguration,
                        validator,
                        meterRegistry,
                        xmlMapper,
                        jsonMapper,
                        conversionHandler);

        dispatcher =
                new Dispatcher(
                        messageHandler,
                        rabbitTemplate,
                        edxlHandler,
                        xmlMapper,
                        jsonMapper,
                        conversionHandler,
                        hubConfiguration,
                        persistenceService);
    }

    @Test
    void dispatchLogsHashWhenReceivingMessage() {
        // Arrange: set up MessageHandler with a ListAppender to capture logs
        Logger logger = (Logger) LoggerFactory.getLogger(MessageHandler.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        // Act: call dispatch
        dispatcher.dispatch(amqpMessage);

        // Assert: check that the first log found in logger contains expected message and hash
        var logs = listAppender.list;
        assertFalse(logs.isEmpty());
        var receivedLogWithHash = logs.getFirst();
        String expectedMessage =
                String.format("Received Report: message with hashed value %s", INPUT_HASH);
        assertEquals(Level.INFO, receivedLogWithHash.getLevel());
        assertTrue(receivedLogWithHash.getMessage().contains(expectedMessage));
    }

    @Test
    void dispatchLogsHashBeforeSendingMessage() {
        // Arrange: set up MessageHandler with a ListAppender to capture logs
        Logger logger = (Logger) LoggerFactory.getLogger(MessageHandler.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        // Act: call dispatch
        dispatcher.dispatch(amqpMessage);

        // Assert: check that the last log found in logger contains expected message and hash
        var logs = listAppender.list;
        assertFalse(logs.isEmpty());
        var receivedLogWithHash = logs.getLast();
        String expectedForwardingLog = "Forwarding";
        String expectedHashValue = String.format("hashed value %s", OUTPUT_HASH);
        assertEquals(Level.INFO, receivedLogWithHash.getLevel());
        assertTrue(receivedLogWithHash.getMessage().contains(expectedForwardingLog));
        assertTrue(receivedLogWithHash.getMessage().contains(expectedHashValue));
    }
}
