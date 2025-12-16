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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.hubsante.hub.config.HubConfiguration;
import com.hubsante.model.EdxlHandler;
import com.hubsante.model.Validator;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePropertiesBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@SpringBootTest(
        classes = {HubConfiguration.class, LogIntegrityTest.TestConfig.class},
        properties = {
            "supported.messages.file=config/supported.messages.csv",
            "client.preferences.file=config/client.preferences.csv",
            "dispatcher.default.ttl=600",
            "spring.rabbitmq.virtual-host=15-15_v2.1"
        })
@RunWith(SpringRunner.class)
public class LogIntegrityTest {

    @Configuration
    static class TestConfig {
        @Bean
        public MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    @Autowired private HubConfiguration hubConfiguration;
    @Autowired private MeterRegistry meterRegistry;
    private RabbitTemplate rabbitTemplate;
    private final XmlMapper xmlMapper = new XmlMapper();
    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final Validator validator = new Validator();
    private final EdxlHandler edxlHandler = new EdxlHandler();
    private Dispatcher dispatcher;

    private Message amqpMessage;

    private ListAppender<ILoggingEvent> listAppender;

    private static final String SENDER_ID = "fr.health.samuV3";
    private static final String DISTRIBUTION_ID =
            "fr.health.samuV3_c89f718b-73e0-4d0a-b8a9-12696bd49522";
    private static final String INPUT_HASH = "9slJ9F2xZCUd/JM2WM9n5+Dk+OnkxrLaHW1CvJmsb78=";
    private static final String INPUT_MESSAGE_TYPE = MessageProperties.CONTENT_TYPE_JSON;

    @BeforeEach
    void setup() throws Exception {
        rabbitTemplate = mock(RabbitTemplate.class);

        // Arrange: Prepare a JSON AMQP message from input file
        MessageProperties props =
                MessagePropertiesBuilder.newInstance().setContentType(INPUT_MESSAGE_TYPE).build();
        props.setReceivedRoutingKey(SENDER_ID);
        props.setReceivedDeliveryMode(MessageDeliveryMode.PERSISTENT);
        byte[] body =
                this.getClass()
                        .getClassLoader()
                        .getResourceAsStream("sample/input_integrity_message")
                        .readAllBytes();
        amqpMessage = new Message(body, props);

        // Arrange: set up MessageHandler and Dispatcher with a ListAppender to capture logs
        Logger logger = (Logger) LoggerFactory.getLogger(MessageHandler.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        // Mock web client to output conversion call body in a local file
        WebClient webClient = mock(WebClient.class);
        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        // Minimal string value returned by conversion API
        Mono<String> responseMono = Mono.just("{\"edxl\": {}}");

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.contentType(any())).thenReturn(requestBodyUriSpec);

        // Capture the bodyValue argument to write it to a file
        doAnswer(
                        invocation -> {
                            String requestBody = invocation.getArgument(0);
                            // Write requestBody to a file under at root of project
                            Path outDir = Paths.get("");
                            Files.createDirectories(outDir);
                            Path outFile = outDir.resolve("conversion-request-body.json");
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
                        hubConfiguration);
    }

    @Test
    void dispatchLogsHashWhenReceivingMessage() {
        // Act: call dispatch
        dispatcher.dispatch(amqpMessage);

        // Assert: check that the first log found in logger contains expected message and hash
        var logs = listAppender.list;
        assertFalse(logs.isEmpty());
        var receivedLogWithHash = logs.getFirst();
        String expectedMessage =
                String.format(
                        " [x] Received Report from '%s': message with distributionId %s and hashed value %s",
                        SENDER_ID, DISTRIBUTION_ID, INPUT_HASH);
        assertEquals(Level.INFO, receivedLogWithHash.getLevel());
        assertTrue(receivedLogWithHash.getMessage().contains(expectedMessage));
    }
}
