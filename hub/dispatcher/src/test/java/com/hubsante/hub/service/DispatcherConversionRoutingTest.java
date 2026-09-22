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
import static com.hubsante.hub.service.ConversionStubs.verifyConversion;
import static com.hubsante.hub.service.ConversionStubs.verifyNoConversion;
import static com.hubsante.hub.testsupport.HubTestConstants.*;
import static com.hubsante.hub.testsupport.HubTestScaffolding.aHub;
import static com.hubsante.hub.testsupport.MessageTestUtils.*;
import static com.hubsante.hub.testsupport.assertions.HubAssertions.assertThatMessageSentTo;
import static com.hubsante.hub.testsupport.assertions.HubAssertions.assertThatMessagesSentTo;
import static com.hubsante.hub.testsupport.assertions.HubAssertions.assertThatNoMessageSentTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.hubsante.hub.config.HubConfiguration;
import com.hubsante.hub.testsupport.HubTestScaffolding;
import com.hubsante.hub.utils.*;
import com.hubsante.model.EdxlHandler;
import com.hubsante.model.edxl.EdxlMessage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@DisplayName("Dispatcher — conversion routing")
class DispatcherConversionRoutingTest {

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
    @DisplayName("should call conversion service for cisu messages")
    public void shouldCallConversionServiceForCisuMessages() throws IOException {
        // CISU bridge fires only when the hub sits on the NEXSIS vhost AND the health recipient is
        // not flagged directCISU. samuA satisfies the latter (directCISU=false in the test CSV).
        doReturn(NEXSIS_VHOST).when(hubConfig).getVhost();

        Message fromFireMessage =
                createMessage("EDXL-DE", XML, SDIS_C_ROUTING_KEY, SAMU_V3_ROUTING_KEY);

        dispatcher.dispatch(fromFireMessage);

        verifyConversion(conversionHandler, ConversionUtils.ConversionType.CISU_TRANSCODING);
    }

    @Test
    @DisplayName("should call conversion service for messages from health to CISU on health vhost")
    public void cisuTranscodingFromHealthToCisuOnHealthVhost() throws IOException {
        Message messageToFire =
                createMessage("EDXL-DE", JSON, SAMU_A_ROUTING_KEY, SDIS_C_ROUTING_KEY);

        dispatcher.dispatch(messageToFire);

        verifyConversion(conversionHandler, ConversionUtils.ConversionType.CISU_TRANSCODING);

        String expectedTargetExchangeName = "transfer_15-15_v2.1_to_15-nexsis_vactive";

        assertThatMessageSentTo(rabbitTemplate, expectedTargetExchangeName, SAMU_A_ROUTING_KEY);
    }

    @Test
    @DisplayName(
            "should not call conversion service for messages from health to CISU on nexsis vhost")
    public void cisuTranscodingFromHealthToCisuOnNexsisVhost() throws IOException {
        doReturn(NEXSIS_VHOST).when(hubConfig).getVhost();
        Message messageToFire =
                createMessage("EDXL-DE", JSON, SAMU_A_ROUTING_KEY, SDIS_C_ROUTING_KEY);

        dispatcher.dispatch(messageToFire);

        verifyNoConversion(conversionHandler);

        assertThatMessageSentTo(rabbitTemplate, DISTRIBUTION_EXCHANGE, SDIS_C_MESSAGE_QUEUE);
    }

    @Test
    @DisplayName("should call conversion service for messages from CISU to health on nexsis vhost")
    public void cisuTranscodingFromCisuToHealthOnNexsisVhost() throws IOException {
        doReturn(NEXSIS_VHOST).when(hubConfig).getVhost();
        Message messageFromFire =
                createMessage("EDXL-DE", JSON, SDIS_C_ROUTING_KEY, SAMU_V3_ROUTING_KEY);
        // Manually override the received routing key to put fr.fire.sga as per the hubex partner
        // shovel configuration
        messageFromFire.getMessageProperties().setReceivedRoutingKey(FIRE_ROUTING_KEY);

        dispatcher.dispatch(messageFromFire);

        verifyConversion(conversionHandler);

        String expectedTargetExchangeName = "transfer_15-nexsis_vactive_to_15-15_v2.1";

        assertThatMessageSentTo(rabbitTemplate, expectedTargetExchangeName, FIRE_ROUTING_KEY);
    }

    @Test
    @DisplayName(
            "should not call conversion service for messages from CISU to health on health vhost")
    public void cisuTranscodingFromCisuToHealthOnHealthVhost() throws IOException {
        Message messageFromFire =
                createMessage("EDXL-DE", JSON, SDIS_C_ROUTING_KEY, SAMU_V3_ROUTING_KEY);
        // Manually override the received routing key to put fr.fire.sga as per the hubex partner
        // shovel configuration
        messageFromFire.getMessageProperties().setReceivedRoutingKey("fr.fire.sga");

        dispatcher.dispatch(messageFromFire);

        verifyNoConversion(conversionHandler);

        assertThatMessageSentTo(rabbitTemplate, DISTRIBUTION_EXCHANGE, SAMU_V3_MESSAGE_QUEUE);
    }

    @Test
    @DisplayName("should call conversion service for messages which need version conversion")
    public void shouldCallConversionServiceForVersionConvertedMessages() throws IOException {
        // samuA -> samuV1 on the default vhost 15-15_v2.1 => conversion triggered
        Message message = createMessage("EDXL-DE", JSON, SAMU_A_ROUTING_KEY, SAMU_V1_ROUTING_KEY);

        dispatcher.dispatch(message);

        verifyConversion(
                conversionHandler, ConversionUtils.ConversionType.HEALTH_VERSION_CONVERSION);
    }

    @Test
    @DisplayName("should not call conversion service for health messages")
    public void shouldNotCallConversionServiceForHealthMessages() throws IOException {
        // samuA -> samuV3 on the default vhost 15-15_v2.1 => no conversion needed.
        Message message = createMessage("EDXL-DE", JSON, SAMU_A_ROUTING_KEY, SAMU_V3_ROUTING_KEY);

        dispatcher.dispatch(message);

        verifyNoConversion(conversionHandler);
    }

    @Test
    @DisplayName(
            "should call conversion service for messages from SAMU which need CISU version conversion")
    public void shouldCallConversionServiceForCISUVersionConvertedMessagesFromSamu()
            throws IOException {
        Message message =
                createMessage("EDXL-DE", JSON, SAMU_V3_DIRECT_CISU_ROUTING_KEY, SDIS_C_ROUTING_KEY);

        doReturn("15-nexsis_v1.9").when(hubConfig).getVhost();
        dispatcher.dispatch(message);

        verifyConversion(conversionHandler, ConversionUtils.ConversionType.CISU_VERSION_CONVERSION);

        String expectedTargetExchangeName = "transfer_15-nexsis_v1.9_to_15-nexsis_vactive";

        assertThatMessageSentTo(
                rabbitTemplate, expectedTargetExchangeName, SAMU_V3_DIRECT_CISU_ROUTING_KEY);
    }

    @Test
    @DisplayName(
            "should call conversion service for messages from NexSIS which need CISU version conversion")
    public void shouldCallConversionServiceForCISUVersionConvertedMessagesFromNexsis()
            throws IOException {
        Message messageFromFire =
                createMessage("EDXL-DE", JSON, SDIS_C_ROUTING_KEY, SAMU_V3_DIRECT_CISU_ROUTING_KEY);
        // Manually override the received routing key to put fr.fire.sga as per the hubex partner
        // shovel configuration
        messageFromFire.getMessageProperties().setReceivedRoutingKey(FIRE_ROUTING_KEY);

        doReturn(NEXSIS_VHOST).when(hubConfig).getVhost();
        dispatcher.dispatch(messageFromFire);

        verifyConversion(conversionHandler, ConversionUtils.ConversionType.CISU_VERSION_CONVERSION);

        String expectedTargetExchangeName = "transfer_15-nexsis_vactive_to_15-nexsis_v1.9";

        assertThatMessageSentTo(rabbitTemplate, expectedTargetExchangeName, FIRE_ROUTING_KEY);
    }

    @Test
    @DisplayName("should send version converted message to transfer exchange")
    public void sendToTransferExchange() throws IOException {
        // samuA -> samuV1 on default vhost 15-15_v2.1; conversion triggered
        Message message = createMessage("EDXL-DE", XML, SAMU_A_ROUTING_KEY, SAMU_V1_ROUTING_KEY);
        EdxlMessage edxlMessage =
                edxlHandler.deserializeXmlEDXL(
                        new String(message.getBody(), StandardCharsets.UTF_8));
        String exchangeName = "transfer_15-15_v2.1_to_15-15_v1.5";

        dispatcher.sendToTransferExchange(message.toString(), message, "15-15_v1.5");

        verify(rabbitTemplate).send(eq(exchangeName), eq(SAMU_A_ROUTING_KEY), any(Message.class));
    }

    @Test
    @DisplayName("should call sendToTransferExchange when there is a version conversion")
    public void transferToOtherVhost() throws IOException {
        Message message = createMessage("EDXL-DE", JSON, SAMU_A_ROUTING_KEY, SAMU_V1_ROUTING_KEY);

        dispatcher.dispatch(message);

        verify(dispatcher, times(1)).sendToTransferExchange(anyString(), any(), any());

        // the message must NOT have been published on the source target queue
        assertThatNoMessageSentTo(rabbitTemplate, DISTRIBUTION_EXCHANGE, SAMU_B_MESSAGE_QUEUE);
    }

    @Test
    @DisplayName("should transfer all messages received from converter as array")
    public void shouldTransferEveryMessageReturnedByConverter() throws IOException {
        Message message = createMessage("EDXL-DE", JSON, SAMU_A_ROUTING_KEY, SAMU_V1_ROUTING_KEY);
        String exchangeName = "transfer_15-15_v2.1_to_15-15_v1.5";

        // Returns a list of 2 converted messages
        echoConversionService(conversionHandler, 2);

        dispatcher.dispatch(message);

        verifyConversion(
                conversionHandler, ConversionUtils.ConversionType.HEALTH_VERSION_CONVERSION);

        assertThatMessagesSentTo(rabbitTemplate, exchangeName, SAMU_A_ROUTING_KEY, 2);
    }
}
