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

import static com.hubsante.hub.config.AmqpConfiguration.DISTRIBUTION_EXCHANGE;
import static com.hubsante.hub.config.AmqpConfiguration.DLQ_REASON;
import static com.hubsante.hub.config.AmqpConfiguration.ORIGINAL_ROUTING_KEY;
import static com.hubsante.hub.config.Constants.DISPATCH_ERROR;
import static com.hubsante.hub.config.Constants.METRIC_MESSAGE_PROCESSING;
import static com.hubsante.hub.testsupport.HubTestConstants.JSON;
import static com.hubsante.hub.testsupport.HubTestConstants.SAMU_A_INFO_QUEUE;
import static com.hubsante.hub.testsupport.HubTestConstants.SAMU_A_ROUTING_KEY;
import static com.hubsante.hub.testsupport.HubTestConstants.SAMU_B_ROUTING_KEY;
import static com.hubsante.hub.testsupport.HubTestConstants.SDIS_C_ROUTING_KEY;
import static com.hubsante.hub.testsupport.HubTestConstants.XML;
import static com.hubsante.hub.testsupport.HubTestScaffolding.aHub;
import static com.hubsante.hub.testsupport.MessageTestUtils.createInvalidMessage;
import static com.hubsante.hub.testsupport.MessageTestUtils.createMessage;
import static com.hubsante.hub.testsupport.assertions.HubAssertions.assertThatMessageSentTo;
import static com.hubsante.hub.testsupport.assertions.HubAssertions.assertThatNoMessageSentTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hubsante.hub.exception.NotAllowedContentTypeException;
import com.hubsante.hub.exception.SchemaNotFoundException;
import com.hubsante.hub.exception.SchemaValidationException;
import com.hubsante.hub.exception.UnroutableMessageException;
import com.hubsante.hub.testsupport.HubTestScaffolding;
import com.hubsante.model.EdxlHandler;
import com.hubsante.model.edxl.EdxlMessage;
import com.hubsante.model.report.ErrorCode;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@DisplayName("MessageHandler")
class MessageHandlerTest {

    private MessageHandler messageHandler;
    private RabbitTemplate rabbitTemplate;
    private ClientPropertiesRegistry clientPropertiesRegistry;
    private MeterRegistry registry;

    @BeforeEach
    void setUp() {
        HubTestScaffolding.Hub hub = aHub().build();
        messageHandler = hub.messageHandler();
        rabbitTemplate = hub.rabbitTemplate();
        clientPropertiesRegistry = hub.clientPropertiesRegistry();
        registry = hub.registry();
    }

    private static final String RESOURCES_INFO_CISU_USE_CASE = "resourcesInfoCisu";
    private static final String RESOURCES_INFO_USE_CASE = "resourcesInfo";

    private static final EdxlHandler EDXL = new EdxlHandler();

    /**
     * Builds the EDXL from JSON rather than from the model classes, so these tests stay readable as
     * data and keep compiling across the model-library versions of the CI matrix. {@code contentKey}
     * is the wrapper's JSON property — {@code resourcesInfo}, {@code resourcesInfoCisu}, … — which is
     * what determines the use case the handler reads.
     */
    private static EdxlMessage messageFor(String recipientId, String contentKey) {
        String json =
                """
                {
                  "distributionID": "%s_1234",
                  "senderID": "%s",
                  "dateTimeSent": "2022-07-25T10:04:34+01:00",
                  "dateTimeExpires": "2072-07-25T10:04:34+01:00",
                  "distributionStatus": "Actual",
                  "distributionKind": "Report",
                  "descriptor": {
                    "language": "fr-FR",
                    "explicitAddress": {
                      "explicitAddressScheme": "hubex",
                      "explicitAddressValue": "%s"
                    }
                  },
                  "content": [
                    { "jsonContent": { "embeddedJsonContent": { "message": { "%s": {} } } } }
                  ]
                }
                """
                        .formatted(SAMU_A_ROUTING_KEY, SAMU_A_ROUTING_KEY, recipientId, contentKey);
        try {
            return EDXL.deserializeJsonEDXL(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not build the test EDXL message", e);
        }
    }

    // ─── inhibitMessageIfNeeded ───────────────────────────────────────────────

    @Nested
    @DisplayName("inhibitMessageIfNeeded")
    class InhibitMessageIfNeeded {

        @Test
        @DisplayName("should throw when the use case is inhibited for the recipient")
        void shouldThrowWhenInhibited() {
            doReturn(List.of("ResourcesInfoCisuWrapper"))
                    .when(clientPropertiesRegistry)
                    .getClientInhibitedUseCases(SAMU_B_ROUTING_KEY);

            assertThatThrownBy(
                            () ->
                                    messageHandler.inhibitMessageIfNeeded(
                                            messageFor(
                                                    SAMU_B_ROUTING_KEY,
                                                    RESOURCES_INFO_CISU_USE_CASE)))
                    .isInstanceOf(UnroutableMessageException.class)
                    .hasMessage(
                            "Use case ResourcesInfoCisuWrapper is not supported for client "
                                    + SAMU_B_ROUTING_KEY);
        }

        @Test
        @DisplayName("should pass when the use case is not among the inhibited ones")
        void shouldPassWhenUseCaseNotInhibited() {
            doReturn(List.of("ResourcesInfoCisuWrapper"))
                    .when(clientPropertiesRegistry)
                    .getClientInhibitedUseCases(SAMU_B_ROUTING_KEY);

            assertThatCode(
                            () ->
                                    messageHandler.inhibitMessageIfNeeded(
                                            messageFor(
                                                    SAMU_B_ROUTING_KEY, RESOURCES_INFO_USE_CASE)))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should pass when the recipient declares no inhibited use case")
        void shouldPassWhenClientHasNoRestriction() {
            assertThatCode(
                            () ->
                                    messageHandler.inhibitMessageIfNeeded(
                                            messageFor(
                                                    SAMU_B_ROUTING_KEY,
                                                    RESOURCES_INFO_CISU_USE_CASE)))
                    .doesNotThrowAnyException();
        }
    }

    // ─── extractMessage ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("extractMessage")
    class ExtractMessage {

        @Test
        @DisplayName("should deserialize a valid JSON message")
        void shouldExtractJson() throws IOException {
            EdxlMessage extracted = messageHandler.extractMessage(createMessage("EDXL-DE", JSON));

            assertThat(extracted.getSenderID()).isEqualTo(SAMU_A_ROUTING_KEY);
        }

        @Test
        @DisplayName("should deserialize a valid XML message")
        void shouldExtractXml() throws IOException {
            EdxlMessage extracted = messageHandler.extractMessage(createMessage("EDXL-DE", XML));

            assertThat(extracted.getSenderID())
                    .as("the XML sample is sent by samuB, unlike the JSON one")
                    .isEqualTo(SAMU_B_ROUTING_KEY);
        }

        @Test
        @DisplayName("should reject a message with no content type")
        void shouldRejectWithoutContentType() throws IOException {
            Message message = createMessage("EDXL-DE", JSON);
            message.getMessageProperties().setContentType(null);

            assertThatThrownBy(() -> messageHandler.extractMessage(message))
                    .isInstanceOf(NotAllowedContentTypeException.class);
        }

        /**
         * An XML body labelled as JSON fails schema <em>lookup</em>, not validation, so the client
         * receives an opaque internal error rather than a message naming the mismatch.
         */
        @Test
        @DisplayName("should reject a message whose body contradicts its content type")
        void shouldRejectMismatchedBody() throws IOException {
            Message message = createMessage("EDXL-DE", XML);
            message.getMessageProperties().setContentType(JSON);

            assertThatThrownBy(() -> messageHandler.extractMessage(message))
                    .isInstanceOf(SchemaNotFoundException.class);
        }

        @Test
        @DisplayName("should reject content that does not match the schema")
        void shouldRejectInvalidContent() throws IOException {
            Message message =
                    createInvalidMessage(
                            "EDXL-DE/invalid-content-valid-envelope.json", SAMU_A_ROUTING_KEY);

            assertThatThrownBy(() -> messageHandler.extractMessage(message))
                    .isInstanceOf(SchemaValidationException.class);
        }

        @Test
        @DisplayName("should count every extraction attempt")
        void shouldCountExtractions() throws IOException {
            messageHandler.extractMessage(createMessage("EDXL-DE", JSON));

            assertThat(registry.find(METRIC_MESSAGE_PROCESSING).counters())
                    .as("an extract counter is published per message")
                    .isNotEmpty();
        }
    }

    // ─── forwardedMessage ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("forwardedMessage")
    class ForwardedMessage {

        @Test
        @DisplayName("should serialize as XML when the recipient asks for it")
        void shouldSerializeAsXmlForXmlClient() throws IOException {
            Message received = createMessage("EDXL-DE", JSON);
            EdxlMessage edxlMessage = messageHandler.extractMessage(received);

            Message forwarded = messageHandler.forwardedMessage(edxlMessage, received);

            assertThat(forwarded.getMessageProperties().getContentType())
                    .as("samuB declares useXML: true in clients.yaml")
                    .isEqualTo(XML);
        }

        @Test
        @DisplayName("should serialize as XML for a fire recipient with no declared preference")
        void shouldDefaultFireRecipientToXml() throws IOException {
            Message received =
                    createMessage("EDXL-DE", JSON, SAMU_A_ROUTING_KEY, SDIS_C_ROUTING_KEY);
            EdxlMessage edxlMessage = messageHandler.extractMessage(received);

            Message forwarded = messageHandler.forwardedMessage(edxlMessage, received);

            assertThat(forwarded.getMessageProperties().getContentType()).isEqualTo(XML);
        }

        @Test
        @DisplayName("should keep the received properties on the forwarded message")
        void shouldKeepReceivedProperties() throws IOException {
            Message received = createMessage("EDXL-DE", JSON);
            received.getMessageProperties().setHeader(ORIGINAL_ROUTING_KEY, SAMU_A_ROUTING_KEY);
            EdxlMessage edxlMessage = messageHandler.extractMessage(received);

            Message forwarded = messageHandler.forwardedMessage(edxlMessage, received);

            assertThat(forwarded.getMessageProperties().<String>getHeader(ORIGINAL_ROUTING_KEY))
                    .isEqualTo(SAMU_A_ROUTING_KEY);
        }

        @Test
        @DisplayName("should forward a already-serialized string without touching its body")
        void shouldForwardStringMessage() throws IOException {
            Message received = createMessage("EDXL-DE", JSON);

            Message forwarded = messageHandler.forwardedStringMessage("{\"a\":1}", received);

            assertThat(new String(forwarded.getBody())).isEqualTo("{\"a\":1}");
        }
    }

    // ─── handleError ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("handleError")
    class HandleError {

        private Message received() throws IOException {
            Message message = createMessage("EDXL-DE", JSON);
            message.getMessageProperties().setHeader(ORIGINAL_ROUTING_KEY, SAMU_A_ROUTING_KEY);
            return message;
        }

        @Test
        @DisplayName(
                "should send an error report to the sender's info queue and reject the message")
        void shouldSendErrorReport() throws IOException {
            Message message = received();

            assertThatThrownBy(
                            () ->
                                    messageHandler.handleError(
                                            new UnroutableMessageException(
                                                    "boom",
                                                    SAMU_A_ROUTING_KEY + "_1234",
                                                    SAMU_B_ROUTING_KEY,
                                                    "ErrorWrapper"),
                                            message))
                    .isInstanceOf(AmqpRejectAndDontRequeueException.class);

            assertThatMessageSentTo(rabbitTemplate, DISTRIBUTION_EXCHANGE, SAMU_A_INFO_QUEUE)
                    .asError()
                    .hasCode(ErrorCode.UNROUTABLE_MESSAGE)
                    .references(SAMU_A_ROUTING_KEY + "_1234")
                    .hasCauseContaining("boom");
        }

        @Test
        @DisplayName("should not report error to a another hubex partner")
        void shouldNotReportToHubexSender() throws IOException {
            Message message = createMessage("EDXL-DE", JSON);
            message.getMessageProperties().setHeader(ORIGINAL_ROUTING_KEY, SDIS_C_ROUTING_KEY);

            assertThatThrownBy(
                            () ->
                                    messageHandler.handleError(
                                            new UnroutableMessageException(
                                                    "boom",
                                                    "id",
                                                    SAMU_B_ROUTING_KEY,
                                                    "ErrorWrapper"),
                                            message))
                    .isInstanceOf(AmqpRejectAndDontRequeueException.class);

            assertThatNoMessageSentTo(
                    rabbitTemplate, DISTRIBUTION_EXCHANGE, SDIS_C_ROUTING_KEY + ".info");
        }

        @Test
        @DisplayName("should not report a message dead-lettered for a reason other than expiration")
        void shouldNotReportInhibitedDlqMessage() throws IOException {
            Message message = received();
            message.getMessageProperties().setHeader(DLQ_REASON, "rejected");

            assertThatThrownBy(
                            () ->
                                    messageHandler.handleError(
                                            new UnroutableMessageException(
                                                    "boom",
                                                    "id",
                                                    SAMU_B_ROUTING_KEY,
                                                    "ErrorWrapper"),
                                            message))
                    .isInstanceOf(AmqpRejectAndDontRequeueException.class);

            assertThatNoMessageSentTo(rabbitTemplate, DISTRIBUTION_EXCHANGE, SAMU_A_INFO_QUEUE);
        }

        @Test
        @DisplayName("should increment the error metric for the sender")
        void shouldIncrementErrorMetric() throws IOException {
            Message message = received();

            assertThatThrownBy(
                            () ->
                                    messageHandler.handleError(
                                            new UnroutableMessageException(
                                                    "boom",
                                                    "id",
                                                    SAMU_B_ROUTING_KEY,
                                                    "ErrorWrapper"),
                                            message))
                    .isInstanceOf(AmqpRejectAndDontRequeueException.class);

            assertThat(registry.find(DISPATCH_ERROR).counters())
                    .as("an error counter is published per rejected message")
                    .isNotEmpty();
        }
    }
}
