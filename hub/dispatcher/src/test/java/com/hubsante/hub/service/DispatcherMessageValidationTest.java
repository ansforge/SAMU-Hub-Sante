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
import static com.hubsante.hub.config.Constants.*;
import static com.hubsante.hub.service.ConversionStubs.echoConversionService;
import static com.hubsante.hub.testsupport.HubTestConstants.*;
import static com.hubsante.hub.testsupport.HubTestScaffolding.aHub;
import static com.hubsante.hub.testsupport.MessageTestUtils.*;
import static com.hubsante.hub.testsupport.assertions.HubAssertions.assertThatMessageSentTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.hubsante.hub.config.HubConfiguration;
import com.hubsante.hub.exception.UnroutableMessageException;
import com.hubsante.hub.testsupport.HubTestScaffolding;
import com.hubsante.hub.utils.*;
import com.hubsante.model.EdxlHandler;
import com.hubsante.model.edxl.EdxlMessage;
import com.hubsante.model.report.ErrorCode;
import io.micrometer.tracing.Tracer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@DisplayName("Dispatcher — message validation")
class DispatcherMessageValidationTest {

    private Dispatcher dispatcher;
    private MessageHandler messageHandler;
    private ConversionHandler conversionHandler;
    private RabbitTemplate rabbitTemplate;
    private MessagePersistenceService persistenceService;
    private HubConfiguration hubConfig;
    private EdxlHandler edxlHandler;
    private XmlMapper xmlMapper;
    private ObjectMapper jsonMapper;

    @BeforeEach
    void setUp() {
        HubTestScaffolding.Hub hub = aHub().build();
        dispatcher = hub.dispatcher();
        messageHandler = hub.messageHandler();
        conversionHandler = hub.conversionHandler();
        rabbitTemplate = hub.rabbitTemplate();
        persistenceService = hub.persistenceService();
        hubConfig = hub.hubConfig();
        edxlHandler = hub.edxlHandler();
        xmlMapper = hub.xmlMapper();
        jsonMapper = hub.jsonMapper();
        echoConversionService(conversionHandler);
    }

    @Test
    @DisplayName("malformed message should throw an exception")
    public void shouldRejectMalformedMessage() throws IOException {

        // we test that the message has been rejected if we can't parse it
        Message receivedMessage =
                createInvalidMessage("EDXL-DE/unparsable-content.json", SAMU_A_ROUTING_KEY);
        assertThrows(
                AmqpRejectAndDontRequeueException.class,
                () -> dispatcher.dispatch(receivedMessage));

        assertErrorHasBeenSent(
                SAMU_A_INFO_QUEUE,
                ErrorCode.UNRECOGNIZED_MESSAGE_FORMAT,
                SAMU_A_DISTRIBUTION_ID,
                "An internal server error has occurred, please contact the administration team");
    }

    @Test
    @DisplayName("message without content-type is rejected")
    public void rejectMessageWithoutContentType() throws IOException {
        // we test that the message has been rejected if the content-type is not set
        Message receivedMessage = createMessage("EDXL-DE", null);
        assertThrows(
                AmqpRejectAndDontRequeueException.class,
                () -> dispatcher.dispatch(receivedMessage));

        // we test that an error report has been sent with the correct error code
        assertErrorHasBeenSent(
                SAMU_A_INFO_QUEUE,
                ErrorCode.NOT_ALLOWED_CONTENT_TYPE,
                SAMU_A_DISTRIBUTION_ID,
                "Unhandled Content-Type ! Message Content-Type should be set at 'application/json' or 'application/xml'");
    }

    @Test
    @DisplayName("message with unhandled content-type is rejected")
    public void rejectMessageWithUnhandledContentType() throws IOException {
        // we test that the message has been rejected if the content-type is neither json nor xml
        Message receivedMessage = createMessage("EDXL-DE", MessageProperties.DEFAULT_CONTENT_TYPE);
        assertThrows(
                AmqpRejectAndDontRequeueException.class,
                () -> dispatcher.dispatch(receivedMessage));

        // we test that an error report has been sent with the correct error code
        assertErrorHasBeenSent(
                SAMU_A_INFO_QUEUE,
                ErrorCode.NOT_ALLOWED_CONTENT_TYPE,
                SAMU_A_DISTRIBUTION_ID,
                "Unhandled Content-Type ! Message Content-Type should be set at 'application/json' or 'application/xml'");
    }

    @Test
    @DisplayName("message body inconsistent with content-type is rejected")
    public void rejectMessageWithInconsistentBody() throws IOException {
        // We create the AMQP message from the JSON file
        Message receivedMessage = createMessage("EDXL-DE", JSON);
        // We override the content type to XML
        receivedMessage.getMessageProperties().setContentType(XML);
        // we test that the message has been rejected if the body is not consistent with the
        // content-type
        assertThrows(
                AmqpRejectAndDontRequeueException.class,
                () -> dispatcher.dispatch(receivedMessage));

        // we test that an error report has been sent with the correct error code
        assertErrorHasBeenSent(
                SAMU_A_INFO_QUEUE,
                ErrorCode.INVALID_MESSAGE,
                SAMU_A_DISTRIBUTION_ID,
                "Something went wrong with the XSD Validator");
    }

    @Test
    @DisplayName("outer routing key inconsistent with sender ID")
    public void outerRoutingKeyInconsistentWithSenderId() throws IOException {
        // we test that the message has been rejected if the sender ID is not consistent with the
        // outer routing key.
        Message receivedMessage = createMessage("EDXL-DE", JSON);
        receivedMessage.getMessageProperties().setReceivedRoutingKey(INCONSISTENT_ROUTING_KEY);
        assertThrows(
                AmqpRejectAndDontRequeueException.class,
                () -> dispatcher.dispatch(receivedMessage));

        assertErrorHasBeenSent(
                INCONSISTENT_ROUTING_KEY + ".info",
                ErrorCode.SENDER_INCONSISTENCY,
                SAMU_A_DISTRIBUTION_ID,
                "message sender is fr.health.samuA",
                "received routing key is fr.health.no-samu");
    }

    @Test
    @DisplayName("should reject message without persistent delivery mode")
    public void rejectMessageWithoutPersistentDeliveryMode() throws IOException {
        Message receivedMessage = createMessage("EDXL-DE", JSON);
        receivedMessage
                .getMessageProperties()
                .setReceivedDeliveryMode(MessageDeliveryMode.NON_PERSISTENT);
        assertThrows(
                AmqpRejectAndDontRequeueException.class,
                () -> dispatcher.dispatch(receivedMessage));

        assertErrorHasBeenSent(
                SAMU_A_INFO_QUEUE,
                ErrorCode.DELIVERY_MODE_INCONSISTENCY,
                SAMU_A_DISTRIBUTION_ID,
                "fr.health.samuA_2608323d-507d-4cbf-bf74-52007f8124ea",
                "non-persistent delivery mode");
    }

    @Test
    @DisplayName("should reject message with invalid json EDXL envelope")
    public void invalidJsonEDXLFails() throws IOException {
        Message receivedMessage =
                createInvalidMessage(
                        "EDXL-DE/missing-EDXL-required-field.json", SAMU_A_ROUTING_KEY);
        assertThrows(
                AmqpRejectAndDontRequeueException.class,
                () -> dispatcher.dispatch(receivedMessage));

        assertErrorHasBeenSent(
                SAMU_A_INFO_QUEUE,
                ErrorCode.INVALID_MESSAGE,
                DISTRIBUTION_ID_UNAVAILABLE,
                "distributionID: is missing but it is required",
                "descriptor.explicitAddress.explicitAddressValue: is missing but it is required");
    }

    @Test
    @DisplayName("should reject message with invalid json content")
    public void invalidJsonContentFails() throws IOException {
        Message receivedMessage =
                createInvalidMessage(
                        "EDXL-DE/invalid-content-valid-envelope.json", JSON, SAMU_A_ROUTING_KEY);

        assertThrows(
                AmqpRejectAndDontRequeueException.class,
                () -> dispatcher.dispatch(receivedMessage));
        assertErrorHasBeenSent(
                SAMU_A_INFO_QUEUE,
                ErrorCode.INVALID_MESSAGE,
                SAMU_A_DISTRIBUTION_ID,
                "reference.invalid_key: is not defined in the schema and the schema does not allow additional properties");
    }

    @Test
    @DisplayName("should reject message with invalid xml content")
    public void invalidXmlContentFails() throws IOException {
        Message receivedMessage =
                createInvalidMessage(
                        "EDXL-DE/invalid-content-valid-envelope.xml", XML, SAMU_B_ROUTING_KEY);

        assertThrows(
                AmqpRejectAndDontRequeueException.class,
                () -> dispatcher.dispatch(receivedMessage));
        assertErrorHasBeenSent(
                SAMU_B_INFO_QUEUE,
                ErrorCode.INVALID_MESSAGE,
                "fr.health.samuB_2608323d-507d-4cbf-bf74-52007f8124ea",
                "Invalid content was found starting with element '{\"urn:emergency:eda:1.9:reference\":reference}'.");
    }

    @Test
    @DisplayName("should not throw when message class is supported")
    public void checkMessageClassNameSupportedDoesNotThrow() throws Exception {
        Message message = createMessage("EDXL-DE", JSON);
        EdxlMessage edxlMessage =
                edxlHandler.deserializeJsonEDXL(
                        new String(message.getBody(), StandardCharsets.UTF_8));

        HubConfiguration hubConfig = mock(HubConfiguration.class);
        String supportedClassName = "SUPPORTED_CLASS";
        when(hubConfig.getSupportedMessages()).thenReturn(List.of(supportedClassName));
        try (MockedStatic<EdxlUtils> mockedEdxlUtils = mockStatic(EdxlUtils.class)) {
            mockedEdxlUtils
                    .when(
                            () ->
                                    EdxlUtils.getUseCaseFromMessage(
                                            edxlMessage.getFirstContentMessage()))
                    .thenReturn(supportedClassName);

            new Dispatcher(
                    messageHandler,
                    rabbitTemplate,
                    edxlHandler,
                    xmlMapper,
                    jsonMapper,
                    conversionHandler,
                    hubConfig,
                    persistenceService,
                    Tracer.NOOP);

            assertDoesNotThrow(
                    () -> MessageUtils.checkMessageClassNameSupported(edxlMessage, hubConfig));
        }
    }

    @Test
    @DisplayName("should throw UnroutableMessageException when message class is not supported")
    public void checkMessageClassNameSupportedThrowsException() throws Exception {
        Message message = createMessage("EDXL-DE", JSON);
        EdxlMessage edxlMessage =
                edxlHandler.deserializeJsonEDXL(
                        new String(message.getBody(), StandardCharsets.UTF_8));

        HubConfiguration hubConfig = mock(HubConfiguration.class);
        String unsupportedClassName = "UNSUPPORTED_CLASS";
        when(hubConfig.getSupportedMessages()).thenReturn(List.of("SUPPORTED_CLASS"));
        when(hubConfig.getVhost()).thenReturn("15-15_v1.5");

        try (MockedStatic<EdxlUtils> mockedEdxlUtils = mockStatic(EdxlUtils.class)) {
            mockedEdxlUtils
                    .when(
                            () ->
                                    EdxlUtils.getUseCaseFromMessage(
                                            edxlMessage.getFirstContentMessage()))
                    .thenReturn(unsupportedClassName);

            new Dispatcher(
                    messageHandler,
                    rabbitTemplate,
                    edxlHandler,
                    xmlMapper,
                    jsonMapper,
                    conversionHandler,
                    hubConfig,
                    persistenceService,
                    Tracer.NOOP);

            UnroutableMessageException thrown =
                    assertThrows(
                            UnroutableMessageException.class,
                            () -> {
                                MessageUtils.checkMessageClassNameSupported(edxlMessage, hubConfig);
                            });
            assertEquals(
                    "The received message classname UNSUPPORTED_CLASS is not supported on the vhost 15-15_v1.5",
                    thrown.getMessage());
        }
    }

    @Disabled("Re-enable when info message sending to outer hubex is restored")
    @Test
    @DisplayName("should reject message if no health actor is involved")
    public void shouldRejectMessageIfNoHealthActorIsInvolved() throws IOException {
        Message receivedMessage =
                createInvalidMessage("EDXL-DE/no-health-actor.json", JSON, "fr.police.random");
        assertThrows(
                AmqpRejectAndDontRequeueException.class,
                () -> dispatcher.dispatch(receivedMessage));
        assertErrorHasBeenSent(
                "fr.police.random.info",
                ErrorCode.UNROUTABLE_MESSAGE,
                "fr.police.random_2608323d-507d-4cbf-bf74-52007f8124ea",
                "Unable to route message with id fr.police.random_2608323d-507d-4cbf-bf74-52007f8124ea, no health actor involved.");
    }

    private void assertErrorHasBeenSent(
            String infoQueueName,
            ErrorCode errorCode,
            String referencedDistributionId,
            String... errorCause) {

        assertThatMessageSentTo(rabbitTemplate, DISTRIBUTION_EXCHANGE, infoQueueName)
                .asError()
                .hasCode(errorCode)
                .references(referencedDistributionId)
                .hasCauseContaining(errorCause);
    }
}
