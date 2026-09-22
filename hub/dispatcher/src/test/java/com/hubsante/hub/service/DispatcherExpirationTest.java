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
import static com.hubsante.hub.testsupport.HubTestConstants.*;
import static com.hubsante.hub.testsupport.HubTestScaffolding.aHub;
import static com.hubsante.hub.testsupport.MessageTestUtils.*;
import static com.hubsante.hub.testsupport.assertions.HubAssertions.assertThatMessageSentTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.hubsante.hub.exception.ExpiredBeforeDispatchMessageException;
import com.hubsante.hub.testsupport.HubTestScaffolding;
import com.hubsante.model.EdxlHandler;
import com.hubsante.model.edxl.EdxlMessage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@DisplayName("Dispatcher — TTL and expiration")
class DispatcherExpirationTest {

    private Dispatcher dispatcher;
    private RabbitTemplate rabbitTemplate;
    private EdxlHandler edxlHandler;

    @BeforeEach
    void setUp() {
        HubTestScaffolding.Hub hub = aHub().build();
        dispatcher = hub.dispatcher();
        rabbitTemplate = hub.rabbitTemplate();
        edxlHandler = hub.edxlHandler();
    }

    @Test
    @DisplayName("should reset TTL if edxl dateTimeExpires is lower")
    public void shouldResetTTL() throws IOException {
        // get message and override dateTimeExpires field with sooner value
        Message base = createMessage("EDXL-DE", JSON);
        EdxlMessage edxlMessage =
                edxlHandler.deserializeJsonEDXL(new String(base.getBody(), StandardCharsets.UTF_8));
        setCustomExpirationDate(edxlMessage, 2);
        Message customTTLMessage =
                new Message(
                        edxlHandler.serializeJsonEDXL(edxlMessage).getBytes(),
                        base.getMessageProperties());

        // before dispatch, the message has no expiration set
        assertNull(customTTLMessage.getMessageProperties().getExpiration());
        // method call
        dispatcher.dispatch(customTTLMessage);
        // we capture the forwarded message to ensure that it has been overwritten
        ArgumentCaptor<Message> argument = ArgumentCaptor.forClass(Message.class);
        Mockito.verify(rabbitTemplate, times(1))
                .send(eq(DISTRIBUTION_EXCHANGE), eq(SAMU_B_MESSAGE_QUEUE), argument.capture());

        // when calling rabbitTemplate.send(), the message has new expiration set
        assertNotNull(argument.getValue().getMessageProperties().getExpiration());
    }

    @Test
    @DisplayName("should send error message if the custom dateTimeExpires is in the past")
    public void shouldThrowExpiredBeforeDispatchMessageException() throws IOException {
        // get message and override dateTimeExpires field with a value in the past
        Message base = createMessage("EDXL-DE", JSON);
        EdxlMessage edxlMessage =
                edxlHandler.deserializeJsonEDXL(new String(base.getBody(), StandardCharsets.UTF_8));
        setCustomExpirationDate(edxlMessage, -2);
        Message customTTLMessage =
                new Message(
                        edxlHandler.serializeJsonEDXL(edxlMessage).getBytes(),
                        base.getMessageProperties());

        // before dispatch, the message has no expiration set
        assertNull(customTTLMessage.getMessageProperties().getExpiration());

        AmqpRejectAndDontRequeueException ex =
                assertThrows(
                        AmqpRejectAndDontRequeueException.class,
                        () -> dispatcher.dispatch(customTTLMessage));
        assertNotNull(ex.getCause());
        assertInstanceOf(
                ExpiredBeforeDispatchMessageException.class,
                ex.getCause(),
                "Cause should be ExpiredBeforeDispatchMessageException");

        assertThatMessageSentTo(rabbitTemplate, DISTRIBUTION_EXCHANGE, SAMU_A_INFO_QUEUE);
    }

    @Test
    @DisplayName("should reset expiration AMQP property expiration to null before dispatching")
    public void shouldResetExpirationPropertyToNullBeforeDispatch() throws IOException {
        // Create a message and set an expiration property
        Message base = createMessage("EDXL-DE", JSON);
        MessageProperties props = base.getMessageProperties();
        props.setExpiration("1000");
        EdxlMessage edxlMessage =
                edxlHandler.deserializeJsonEDXL(new String(base.getBody(), StandardCharsets.UTF_8));
        Message customTTLMessage =
                new Message(edxlHandler.serializeJsonEDXL(edxlMessage).getBytes(), props);

        // Ensure the expiration property is set before dispatch
        assertEquals("1000", customTTLMessage.getMessageProperties().getExpiration());

        // Dispatch the message
        dispatcher.dispatch(customTTLMessage);

        // Capture the forwarded message and check that expiration is null (reset)
        ArgumentCaptor<Message> argument = ArgumentCaptor.forClass(Message.class);
        Mockito.verify(rabbitTemplate, times(1))
                .send(eq(DISTRIBUTION_EXCHANGE), eq(SAMU_B_MESSAGE_QUEUE), argument.capture());
        Message sentMessage = argument.getValue();
        assertNull(sentMessage.getMessageProperties().getExpiration());
    }
}
