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
import static com.hubsante.hub.service.ConversionStubs.verifyNoConversion;
import static com.hubsante.hub.testsupport.HubTestConstants.*;
import static com.hubsante.hub.testsupport.HubTestScaffolding.aHub;
import static com.hubsante.hub.testsupport.MessageTestUtils.*;
import static com.hubsante.hub.testsupport.assertions.HubAssertions.assertThatMessageSentTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.hubsante.hub.config.HubConfiguration;
import com.hubsante.hub.testsupport.HubTestScaffolding;
import com.hubsante.model.EdxlHandler;
import com.hubsante.model.edxl.EdxlMessage;
import com.hubsante.model.technical.noreq.TechnicalNoreqWrapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@DisplayName("Dispatcher — routing to recipient queues")
class DispatcherRoutingTest {

    private Dispatcher dispatcher;
    private ConversionHandler conversionHandler;
    private RabbitTemplate rabbitTemplate;
    private HubConfiguration hubConfig;
    private EdxlHandler edxlHandler;

    @BeforeEach
    void setUp() {
        HubTestScaffolding.Hub hub = aHub().build();
        dispatcher = hub.dispatcher();
        conversionHandler = hub.conversionHandler();
        rabbitTemplate = hub.rabbitTemplate();
        hubConfig = hub.hubConfig();
        edxlHandler = hub.edxlHandler();
        echoConversionService(conversionHandler);
    }

    @Test
    @DisplayName("should send json message to the right exchange and routing key")
    public void shouldDispatchJsonToRightExchange() throws IOException {
        // generate input message and check that it has the expected content type
        Message receivedMessage = createMessage("EDXL-DE", JSON);
        assertEquals(JSON, receivedMessage.getMessageProperties().getContentType());
        // dispatch message
        dispatcher.dispatch(receivedMessage);
        ArgumentCaptor<Message> argCaptor = ArgumentCaptor.forClass(Message.class);
        // assert that the message was sent to the right exchange with the right routing key
        // exactly 1 time
        Mockito.verify(rabbitTemplate, times(1))
                .send(eq(DISTRIBUTION_EXCHANGE), eq(SAMU_B_MESSAGE_QUEUE), argCaptor.capture());
        // assert that the message has been converted according to the recipient preferences
        Message sentMessage = argCaptor.getValue();
        assertEquals(XML, sentMessage.getMessageProperties().getContentType());
        // assert that the message has the same content as the original one
        EdxlMessage publishedJSON =
                edxlHandler.deserializeJsonEDXL(
                        new String(receivedMessage.getBody(), StandardCharsets.UTF_8));
        EdxlMessage sentXML =
                edxlHandler.deserializeXmlEDXL(
                        new String(sentMessage.getBody(), StandardCharsets.UTF_8));
        assertEquals(publishedJSON, sentXML);

        TechnicalNoreqWrapper custom = (TechnicalNoreqWrapper) sentXML.getFirstContentMessage();
        assertEquals("value", custom.getTechnicalNoreq().getOptionalStringField());
    }

    @Test
    @DisplayName("should send xml message to the right exchange and routing key")
    public void shouldDispatchXmlToRightExchange() throws IOException {
        // generate input message and check that it has the expected content type
        Message receivedMessage = createMessage("EDXL-DE", XML, SAMU_B_ROUTING_KEY);
        assertEquals(XML, receivedMessage.getMessageProperties().getContentType());
        // dispatch message
        dispatcher.dispatch(receivedMessage);
        ArgumentCaptor<Message> argCaptor = ArgumentCaptor.forClass(Message.class);
        // assert that the message was sent to the right exchange with the right routing key
        // exactly 1 time
        Mockito.verify(rabbitTemplate, times(1))
                .send(eq(DISTRIBUTION_EXCHANGE), eq(SAMU_A_MESSAGE_QUEUE), argCaptor.capture());
        // assert that the message has been converted according to the recipient preferences
        Message sentMessage = argCaptor.getValue();
        assertEquals(JSON, sentMessage.getMessageProperties().getContentType());
        // assert that the message has the same content as the original one
        EdxlMessage publishedXML =
                edxlHandler.deserializeXmlEDXL(
                        new String(receivedMessage.getBody(), StandardCharsets.UTF_8));
        EdxlMessage sentJSON =
                edxlHandler.deserializeJsonEDXL(
                        new String(sentMessage.getBody(), StandardCharsets.UTF_8));
        assertEquals(publishedXML, sentJSON);

        TechnicalNoreqWrapper custom = (TechnicalNoreqWrapper) sentJSON.getFirstContentMessage();
        assertEquals("value", custom.getTechnicalNoreq().getOptionalStringField());
    }

    @Test
    @DisplayName("should convert messages according to client preferences")
    public void shouldConvertMessageAccordingToUseXmlPreferences() throws IOException {
        // JSON -> XML direction
        Message receivedJsonMessage = createMessage("EDXL-DE", JSON);
        assertEquals(JSON, receivedJsonMessage.getMessageProperties().getContentType());

        dispatcher.dispatch(receivedJsonMessage);

        ArgumentCaptor<Message> argCaptor = ArgumentCaptor.forClass(Message.class);
        Mockito.verify(rabbitTemplate, times(1))
                .send(eq(DISTRIBUTION_EXCHANGE), eq(SAMU_B_MESSAGE_QUEUE), argCaptor.capture());
        Message sentXmlMessage = argCaptor.getValue();
        assertEquals(XML, sentXmlMessage.getMessageProperties().getContentType());

        // XML -> JSON direction
        Message receivedXMLMessage = createMessage("EDXL-DE", XML, SAMU_B_ROUTING_KEY);
        assertEquals(XML, receivedXMLMessage.getMessageProperties().getContentType());

        dispatcher.dispatch(receivedXMLMessage);

        Mockito.verify(rabbitTemplate, times(1))
                .send(eq(DISTRIBUTION_EXCHANGE), eq(SAMU_A_MESSAGE_QUEUE), argCaptor.capture());
        assertEquals(JSON, argCaptor.getValue().getMessageProperties().getContentType());
    }

    @ParameterizedTest
    @ValueSource(strings = {"15-sas_v1.0", "15-smur_v1.7", "15-gps_v2.0", "15-notexisting_v1.0"})
    @DisplayName("should send message to current vhost")
    public void shouldSendMessageToCurrentVhost(String vhost) throws IOException {
        Message message = createMessage("EDXL-DE", JSON, SAMU_A_ROUTING_KEY, SAMU_V3_ROUTING_KEY);

        doReturn(vhost).when(hubConfig).getVhost();
        dispatcher.dispatch(message);

        verifyNoConversion(conversionHandler);

        assertThatMessageSentTo(rabbitTemplate, DISTRIBUTION_EXCHANGE, SAMU_V3_MESSAGE_QUEUE);
    }
}
