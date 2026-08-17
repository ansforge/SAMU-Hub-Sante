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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.hubsante.hub.testsupport.HubTestScaffolding;
import com.hubsante.model.report.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@DisplayName("Dispatcher — dead letter queue")
class DispatcherDeadLetterTest {

    private Dispatcher dispatcher;
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void setUp() {
        HubTestScaffolding.Hub hub = aHub().build();
        dispatcher = hub.dispatcher();
        rabbitTemplate = hub.rabbitTemplate();
    }

    @Test
    @DisplayName("should send info to sender of DLQed message - expiration")
    public void handleDLQMessage() throws Exception {
        // we test that an expired message has been rejected after the DLQ listener has been called
        Message originalMessage = createMessage("EDXL-DE", JSON);
        Message dlqMessage = applyRabbitmqDLQHeaders(originalMessage, DLQ_EXPIRED_REASON);
        assertThrows(
                AmqpRejectAndDontRequeueException.class, () -> dispatcher.dispatchDLQ(dlqMessage));

        // we test that an error report has been sent with the correct error code
        assertErrorHasBeenSent(
                SAMU_A_INFO_QUEUE,
                ErrorCode.DEAD_LETTER_QUEUED,
                SAMU_A_DISTRIBUTION_ID,
                "fr.health.samuA_2608323d-507d-4cbf-bf74-52007f8124ea",
                "has been read from dead-letter-queue; reason was expired");
    }

    @Test
    @DisplayName("should send info to sender of DLQed message - rejection")
    public void handleDLQRejectedMessage() throws Exception {
        // we test that a rejected message received on the DLQ listener does not trigger a RS-ERROR
        Message originalMessage = createMessage("EDXL-DE", JSON);
        Message dlqMessage = applyRabbitmqDLQHeaders(originalMessage, DLQ_REJECTED_REASON);
        ArgumentCaptor<Message> argCaptor = ArgumentCaptor.forClass(Message.class);
        assertThrows(
                AmqpRejectAndDontRequeueException.class, () -> dispatcher.dispatchDLQ(dlqMessage));

        // No message has been published on sender info queue
        Mockito.verify(rabbitTemplate, times(0))
                .send(eq(DISTRIBUTION_EXCHANGE), eq(SAMU_A_INFO_QUEUE), argCaptor.capture());
    }

    @Test
    @DisplayName("should not send info if info itself is DLQed")
    public void handleDLQInfo() throws Exception {
        Message originalInfo = createMessage("custom-error", JSON, SAMU_A_INFO_QUEUE);
        Message dlqMessage = applyRabbitmqDLQHeaders(originalInfo, "expired");

        assertDoesNotThrow(() -> dispatcher.dispatchDLQ(dlqMessage));
        Mockito.verify(rabbitTemplate, times(0))
                .send(eq(DISTRIBUTION_EXCHANGE), any(), any(Message.class));
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
