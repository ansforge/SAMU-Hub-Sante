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
package com.hubsante.hub.testsupport.assertions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

public final class HubAssertions {

    private HubAssertions() {}

    public static SentMessageAssert assertThatMessageSentTo(
            RabbitTemplate rabbitTemplate, String exchange, String routingKey) {
        return assertThatMessagesSentTo(rabbitTemplate, exchange, routingKey, 1)[0];
    }

    public static SentMessageAssert[] assertThatMessagesSentTo(
            RabbitTemplate rabbitTemplate, String exchange, String routingKey, int expectedCount) {
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate, times(expectedCount))
                .send(eq(exchange), eq(routingKey), captor.capture());
        return captor.getAllValues().stream()
                .map(SentMessageAssert::new)
                .toArray(SentMessageAssert[]::new);
    }

    public static void assertThatNoMessageSentTo(
            RabbitTemplate rabbitTemplate, String exchange, String routingKey) {
        verify(rabbitTemplate, never()).send(eq(exchange), eq(routingKey), any(Message.class));
    }
}
