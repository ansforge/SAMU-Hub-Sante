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

import static com.hubsante.hub.config.AmqpConfiguration.*;
import static com.hubsante.hub.service.ConversionStubs.echoConversionService;
import static com.hubsante.hub.service.ConversionStubs.failConversionService;
import static com.hubsante.hub.testsupport.HubTestConstants.*;
import static com.hubsante.hub.testsupport.HubTestScaffolding.aHub;
import static com.hubsante.hub.testsupport.MessageTestUtils.*;
import static com.hubsante.hub.testsupport.assertions.HubAssertions.assertThatMessageSentTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.hubsante.hub.config.HubConfiguration;
import com.hubsante.hub.exception.ConversionException;
import com.hubsante.hub.exception.SchemaValidationException;
import com.hubsante.hub.testsupport.HubTestScaffolding;
import com.hubsante.model.EdxlHandler;
import com.hubsante.model.Validator;
import com.hubsante.model.edxl.EdxlMessage;
import com.hubsante.model.exception.ValidationException;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@DisplayName("Dispatcher — Conversion - Error Handling")
class DispatcherConversionErrorHandlingTest {

    private Dispatcher dispatcher;
    private MessageHandler messageHandler;
    private ConversionHandler conversionHandler;
    private RabbitTemplate rabbitTemplate;
    private MessagePersistenceService persistenceService;
    private HubConfiguration hubConfig;
    private ClientPropertiesRegistry clientPropertiesRegistry;
    private Validator validator;
    private EdxlHandler edxlHandler;
    private XmlMapper xmlMapper;
    private ObjectMapper jsonMapper;
    private MeterRegistry registry;

    @BeforeEach
    void setUp() {
        HubTestScaffolding.Hub hub = aHub().build();
        dispatcher = hub.dispatcher();
        messageHandler = hub.messageHandler();
        conversionHandler = hub.conversionHandler();
        rabbitTemplate = hub.rabbitTemplate();
        persistenceService = hub.persistenceService();
        hubConfig = hub.hubConfig();
        clientPropertiesRegistry = hub.clientPropertiesRegistry();
        validator = hub.validator();
        edxlHandler = hub.edxlHandler();
        xmlMapper = hub.xmlMapper();
        jsonMapper = hub.jsonMapper();
        registry = hub.registry();
        echoConversionService(conversionHandler);
    }

    @Test
    @DisplayName("should handle conversion service error correctly")
    public void shouldHandleConversionServiceError() throws IOException {
        // sdisC -> samuV3 on vhost 15-nexsis_v1.9 => transcoding triggered
        doReturn(NEXSIS_VHOST).when(hubConfig).getVhost();

        Message receivedMessage =
                createMessage("EDXL-DE", JSON, SDIS_C_ROUTING_KEY, SAMU_V3_ROUTING_KEY);
        EdxlMessage edxlMessage =
                edxlHandler.deserializeJsonEDXL(
                        new String(receivedMessage.getBody(), StandardCharsets.UTF_8));

        String conversionErrorMessage = "Conversion service error message";
        failConversionService(
                conversionHandler,
                new ConversionException(conversionErrorMessage, edxlMessage.getDistributionID()));

        assertThrows(
                AmqpRejectAndDontRequeueException.class,
                () -> dispatcher.dispatch(receivedMessage));

        ArgumentCaptor<ConversionException> exceptionCaptor =
                ArgumentCaptor.forClass(ConversionException.class);
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);

        verify(messageHandler).handleError(exceptionCaptor.capture(), messageCaptor.capture());

        ConversionException thrownException = exceptionCaptor.getValue();
        assertEquals(
                edxlMessage.getDistributionID(), thrownException.getReferencedDistributionID());
        assertTrue(thrownException.getMessage().contains(conversionErrorMessage));

        Message handledMessage = messageCaptor.getValue();
        assertEquals(receivedMessage, handledMessage);
    }

    @Test
    @DisplayName("should transfer to another vhost when an error is raised after message transfer")
    public void transferErrorToOtherVhost() throws IOException, ValidationException {
        doReturn("15-15_v2.0").when(hubConfig).getVhost();
        doReturn(new String[] {"1.5"})
                .when(clientPropertiesRegistry)
                .getClientVersionsForPerimeter(SAMU_A_ROUTING_KEY, "15-15");
        doThrow(
                        new SchemaValidationException(
                                "Mock schema validation error", "mock_distribution_id"))
                .when(validator)
                .validateJSON(anyString(), any());

        Message message = createMessage("EDXL-DE", JSON, SAMU_V1_ROUTING_KEY, SAMU_A_ROUTING_KEY);

        String exchangeName = "transfer_15-15_v2.0_to_15-15_v1.5";

        // Mock call to converter (return same payload for error message)
        AmqpRejectAndDontRequeueException errorThrown =
                assertThrows(
                        AmqpRejectAndDontRequeueException.class,
                        () -> dispatcher.dispatch(message));

        assertEquals("Mock schema validation error", errorThrown.getCause().getMessage());

        assertThatMessageSentTo(rabbitTemplate, exchangeName, "fr.health.hub");
    }

    @Test
    @DisplayName("should forward error message directly when error is received after conversion")
    public void sendErrorMessageToSameVhost() throws IOException {
        Message errorMessage = createMessage("hub-error-to-samuA", JSON);

        dispatcher.dispatch(errorMessage);

        assertThatMessageSentTo(rabbitTemplate, DISTRIBUTION_EXCHANGE, SAMU_A_INFO_QUEUE);
    }

    @Test
    @DisplayName("should send error message to sender info queue when error is raised")
    public void sendErrorMessageWhenErrorIsRaised() throws IOException, ValidationException {
        doReturn("15-15_v1.5").when(hubConfig).getVhost();
        doReturn(new String[] {"1.5"})
                .when(clientPropertiesRegistry)
                .getClientVersionsForPerimeter(SAMU_A_ROUTING_KEY, "15-15");
        // Default hub vhost is v2.1 and samuA declares v2.1: validation error is forwarded directly
        // to samuA's info queue without any conversion.
        doThrow(
                        new SchemaValidationException(
                                "Mock schema validation error", "mock_distribution_id"))
                .when(validator)
                .validateJSON(anyString(), any());

        Message message = createMessage("EDXL-DE", JSON);

        AmqpRejectAndDontRequeueException errorThrown =
                assertThrows(
                        AmqpRejectAndDontRequeueException.class,
                        () -> dispatcher.dispatch(message));

        assertEquals("Mock schema validation error", errorThrown.getCause().getMessage());

        assertThatMessageSentTo(rabbitTemplate, DISTRIBUTION_EXCHANGE, SAMU_A_INFO_QUEUE);
    }

    @Test
    @DisplayName("should log referencedDistributionID for ACK ReferenceWrapper")
    public void shouldLogReferencedDistributionIdForAckReferenceWrapper() throws Exception {
        Message message = createMessage("rc-ref", JSON);

        // Log capturing setup
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MessageHandler.class);
        ch.qos.logback.classic.Logger logbackLogger = (ch.qos.logback.classic.Logger) logger;
        ch.qos.logback.classic.Level originalLevel = logbackLogger.getLevel();
        logbackLogger.setLevel(ch.qos.logback.classic.Level.INFO);
        ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender =
                new ch.qos.logback.core.read.ListAppender<>();
        appender.start();
        logbackLogger.addAppender(appender);

        dispatcher.dispatch(message);

        boolean foundReceivedLog =
                appender.list.stream()
                        .anyMatch(
                                event ->
                                        event.getFormattedMessage()
                                                .contains(
                                                        "Received Ack: message with referenced distributionId fr.health.samuB_2607723d-507d-4cbf-bf74-12345f7064cd"));
        assertTrue(
                foundReceivedLog,
                "Received log should contain referenced distributionId: fr.health.samuB_2607723d-507d-4cbf-bf74-12345f7064cd");

        boolean foundForwardingLog =
                appender.list.stream()
                        .anyMatch(
                                event ->
                                        event.getFormattedMessage()
                                                .contains(
                                                        "Forwarding Ack: message with referenced distributionId fr.health.samuB_2607723d-507d-4cbf-bf74-12345f7064cd"));
        assertTrue(
                foundForwardingLog,
                "Forwarding log should contain referenced distributionId: fr.health.samuB_2607723d-507d-4cbf-bf74-12345f7064cd");

        // Cleanup
        logbackLogger.detachAppender(appender);
        logbackLogger.setLevel(originalLevel);
    }
}
