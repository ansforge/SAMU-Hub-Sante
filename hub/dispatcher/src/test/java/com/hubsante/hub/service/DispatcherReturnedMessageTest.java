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
import static com.hubsante.hub.testsupport.assertions.HubAssertions.assertThatNoMessageSentTo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.verify;

import com.hubsante.hub.testsupport.HubTestScaffolding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@DisplayName("Dispatcher — returned messages")
class DispatcherReturnedMessageTest {

    private RabbitTemplate rabbitTemplate;
    private RabbitTemplate.ReturnsCallback returnsCallback;

    @BeforeEach
    void setUp() {
        HubTestScaffolding.Hub hub = aHub().build();
        rabbitTemplate = hub.rabbitTemplate();
        ArgumentCaptor<RabbitTemplate.ReturnsCallback> captor =
                ArgumentCaptor.forClass(RabbitTemplate.ReturnsCallback.class);
        verify(rabbitTemplate).setReturnsCallback(captor.capture());
        returnsCallback = captor.getValue();
    }

    @Test
    @DisplayName("should send info to the original sender of a returned message")
    void shouldReportReturnedMessageToSender() throws Exception {
        Message message = createMessage("EDXL-DE", JSON);
        message.getMessageProperties().setHeader(ORIGINAL_ROUTING_KEY, SAMU_A_ROUTING_KEY);

        returnsCallback.returnedMessage(returned(message));

        assertThatMessageSentTo(rabbitTemplate, DISTRIBUTION_EXCHANGE, SAMU_A_INFO_QUEUE);
    }

    /**
     * Error reports built by the hub carry no original routing key header: when one is returned,
     * the callback must not fail and must not report it back.
     */
    @Test
    @DisplayName("should handle a returned message carrying no original routing key")
    void shouldHandleReturnedMessageWithoutOriginalRoutingKey() throws Exception {
        Message message = createMessage("EDXL-DE", JSON);

        assertDoesNotThrow(() -> returnsCallback.returnedMessage(returned(message)));

        assertThatNoMessageSentTo(rabbitTemplate, DISTRIBUTION_EXCHANGE, SAMU_A_INFO_QUEUE);
    }

    private static ReturnedMessage returned(Message message) {
        return new ReturnedMessage(message, 312, "NO_ROUTE", DISTRIBUTION_EXCHANGE, "unroutable");
    }
}
