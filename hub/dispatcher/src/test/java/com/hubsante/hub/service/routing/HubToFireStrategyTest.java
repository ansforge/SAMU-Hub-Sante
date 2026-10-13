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
package com.hubsante.hub.service.routing;

import static com.hubsante.hub.config.AmqpConfiguration.DISTRIBUTION_EXCHANGE;
import static com.hubsante.hub.service.ConversionStubs.echoConversionService;
import static com.hubsante.hub.service.ConversionStubs.verifyConversion;
import static com.hubsante.hub.testsupport.HubTestConstants.*;
import static com.hubsante.hub.testsupport.HubTestScaffolding.aHub;
import static com.hubsante.hub.testsupport.MessageTestUtils.createMessage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

import com.hubsante.hub.config.HubConfiguration;
import com.hubsante.hub.exception.DeliveryModeInconsistencyException;
import com.hubsante.hub.exception.InvalidDistributionIDException;
import com.hubsante.hub.exception.SenderInconsistencyException;
import com.hubsante.hub.exception.UnroutableMessageException;
import com.hubsante.hub.service.ClientPropertiesRegistry;
import com.hubsante.hub.service.ConversionHandler;
import com.hubsante.hub.testsupport.HubTestScaffolding;
import com.hubsante.hub.utils.ConversionUtils;
import com.hubsante.model.EdxlHandler;
import com.hubsante.model.edxl.Descriptor;
import com.hubsante.model.edxl.EdxlMessage;
import com.hubsante.model.edxl.ExplicitAddress;
import com.hubsante.model.report.ErrorWrapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;

/** Unit tests for {@link HubToFireStrategy}. */
@DisplayName("HubToFireStrategy")
class HubToFireStrategyTest {

    private static final String DISTRIBUTION_ID = SAMU_A_ROUTING_KEY + "_1234";
    private static final EdxlHandler EDXL = new EdxlHandler();

    private HubToFireStrategy strategy;
    private ConversionHandler conversionHandler;
    private HubConfiguration hubConfig;
    private ClientPropertiesRegistry clientPropertiesRegistry;

    @BeforeEach
    void setUp() {
        HubTestScaffolding.Hub hub = aHub().build();
        strategy =
                new HubToFireStrategy(
                        hub.messageHandler(),
                        hub.conversionHandler(),
                        hub.hubConfig(),
                        hub.persistenceService());
        conversionHandler = hub.conversionHandler();
        hubConfig = hub.hubConfig();
        clientPropertiesRegistry = hub.clientPropertiesRegistry();
        echoConversionService(conversionHandler);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private static EdxlMessage edxl(String senderId, String recipientId, String distributionId) {
        EdxlMessage edxlMessage = new EdxlMessage();
        edxlMessage.setSenderID(senderId);
        edxlMessage.setDistributionID(distributionId);
        edxlMessage.setDescriptor(
                new Descriptor("fr-FR", new ExplicitAddress("hubex", recipientId)));
        edxlMessage.setContentFrom(new ErrorWrapper());
        return edxlMessage;
    }

    private static Message amqp(String receivedRoutingKey) {
        return amqp(receivedRoutingKey, MessageDeliveryMode.PERSISTENT);
    }

    private static Message amqp(String receivedRoutingKey, MessageDeliveryMode deliveryMode) {
        MessageProperties properties = new MessageProperties();
        properties.setReceivedRoutingKey(receivedRoutingKey);
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setReceivedDeliveryMode(deliveryMode);
        return new Message("{}".getBytes(StandardCharsets.UTF_8), properties);
    }

    private static EdxlMessage deserialize(Message message) throws IOException {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        return MessageProperties.CONTENT_TYPE_XML.equals(
                        message.getMessageProperties().getContentType())
                ? EDXL.deserializeXmlEDXL(body)
                : EDXL.deserializeJsonEDXL(body);
    }

    // ─── checkMessageContent ────────────────────────────────────────────────────

    @Nested
    @DisplayName("checkMessageContent")
    class CheckMessageContent {

        @Test
        @DisplayName("should pass for a valid message")
        void shouldPassForAValidMessage() {
            assertThatCode(
                            () ->
                                    strategy.checkMessageContent(
                                            amqp(SAMU_A_ROUTING_KEY),
                                            edxl(
                                                    SAMU_A_ROUTING_KEY,
                                                    SDIS_C_ROUTING_KEY,
                                                    DISTRIBUTION_ID)))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should throw when the message class is not supported on the vhost")
        void shouldThrowWhenMessageClassNotSupported() {
            doReturn(List.of("SomethingElseWrapper")).when(hubConfig).getSupportedMessages();

            assertThatThrownBy(
                            () ->
                                    strategy.checkMessageContent(
                                            amqp(SAMU_A_ROUTING_KEY),
                                            edxl(
                                                    SAMU_A_ROUTING_KEY,
                                                    SDIS_C_ROUTING_KEY,
                                                    DISTRIBUTION_ID)))
                    .isInstanceOf(UnroutableMessageException.class);
        }

        @Test
        @DisplayName("should throw when the use case is inhibited for the recipient")
        void shouldThrowWhenUseCaseInhibited() {
            doReturn(List.of("ErrorWrapper"))
                    .when(clientPropertiesRegistry)
                    .getClientInhibitedUseCases(SDIS_C_ROUTING_KEY);

            assertThatThrownBy(
                            () ->
                                    strategy.checkMessageContent(
                                            amqp(SAMU_A_ROUTING_KEY),
                                            edxl(
                                                    SAMU_A_ROUTING_KEY,
                                                    SDIS_C_ROUTING_KEY,
                                                    DISTRIBUTION_ID)))
                    .isInstanceOf(UnroutableMessageException.class)
                    .hasMessageContaining("not supported for client");
        }

        @Test
        @DisplayName("should throw when no health actor is involved")
        void shouldThrowWhenNoHealthActorInvolved() {
            String distributionId = SDIS_C_ROUTING_KEY + "_1234";

            assertThatThrownBy(
                            () ->
                                    strategy.checkMessageContent(
                                            amqp(SDIS_C_ROUTING_KEY),
                                            edxl(
                                                    SDIS_C_ROUTING_KEY,
                                                    FIRE_ROUTING_KEY,
                                                    distributionId)))
                    .isInstanceOf(UnroutableMessageException.class)
                    .hasMessageContaining("no health actor involved");
        }

        @Test
        @DisplayName("should throw when the health sender is inconsistent with the routing key")
        void shouldThrowWhenSenderInconsistent() {
            assertThatThrownBy(
                            () ->
                                    strategy.checkMessageContent(
                                            amqp("fr.health.someoneElse"),
                                            edxl(
                                                    SAMU_A_ROUTING_KEY,
                                                    SDIS_C_ROUTING_KEY,
                                                    DISTRIBUTION_ID)))
                    .isInstanceOf(SenderInconsistencyException.class);
        }

        @Test
        @DisplayName("should throw when the delivery mode is not persistent")
        void shouldThrowWhenDeliveryModeNotPersistent() {
            assertThatThrownBy(
                            () ->
                                    strategy.checkMessageContent(
                                            amqp(
                                                    SAMU_A_ROUTING_KEY,
                                                    MessageDeliveryMode.NON_PERSISTENT),
                                            edxl(
                                                    SAMU_A_ROUTING_KEY,
                                                    SDIS_C_ROUTING_KEY,
                                                    DISTRIBUTION_ID)))
                    .isInstanceOf(DeliveryModeInconsistencyException.class);
        }

        @Test
        @DisplayName("should throw when the distributionId format is invalid")
        void shouldThrowWhenDistributionIdFormatInvalid() {
            String inconsistentDistributionId = "fr.health.someoneElse_1234";

            assertThatThrownBy(
                            () ->
                                    strategy.checkMessageContent(
                                            amqp(SAMU_A_ROUTING_KEY),
                                            edxl(
                                                    SAMU_A_ROUTING_KEY,
                                                    SDIS_C_ROUTING_KEY,
                                                    inconsistentDistributionId)))
                    .isInstanceOf(InvalidDistributionIDException.class);
        }
    }

    // ─── buildMessageRoutingDTO ─────────────────────────────────────────────────

    @Nested
    @DisplayName("buildMessageRoutingDTO")
    class BuildMessageRoutingDTO {

        @Test
        @DisplayName("should transcode onto the NexSIS vhost when sent from a health vhost")
        void shouldBuildTransferRoutingDTOForCisuTranscoding() throws IOException {
            Message message =
                    createMessage("EDXL-DE", JSON, SAMU_A_ROUTING_KEY, SDIS_C_ROUTING_KEY);
            EdxlMessage edxlMessage = deserialize(message);

            List<MessageRoutingDTO> routingDTOs =
                    strategy.buildMessageRoutingDTO(message, edxlMessage);

            assertThat(routingDTOs).hasSize(1);
            MessageRoutingDTO routingDTO = routingDTOs.getFirst();
            assertThat(routingDTO.destinationExchange())
                    .isEqualTo("transfer_15-15_v2.1_to_15-nexsis_vactive");
            assertThat(routingDTO.routingKey()).isEqualTo(SAMU_A_ROUTING_KEY);

            verifyConversion(conversionHandler, ConversionUtils.ConversionType.CISU_TRANSCODING);
        }

        @Test
        @DisplayName("should build a direct routing DTO when already on the NexSIS vhost")
        void shouldBuildDirectRoutingDTOWhenAlreadyOnNexsisVhost() throws IOException {
            doReturn(NEXSIS_VHOST).when(hubConfig).getVhost();
            Message message =
                    createMessage("EDXL-DE", JSON, SAMU_A_ROUTING_KEY, SDIS_C_ROUTING_KEY);
            EdxlMessage edxlMessage = deserialize(message);

            List<MessageRoutingDTO> routingDTOs =
                    strategy.buildMessageRoutingDTO(message, edxlMessage);

            assertThat(routingDTOs).hasSize(1);
            assertThat(routingDTOs.getFirst().destinationExchange())
                    .isEqualTo(DISTRIBUTION_EXCHANGE);
        }
    }
}
