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

import static com.hubsante.hub.config.Constants.*;
import static com.hubsante.hub.testsupport.HubTestConstants.*;
import static com.hubsante.hub.testsupport.HubTestScaffolding.aHub;
import static com.hubsante.hub.testsupport.MessageTestUtils.*;
import static com.hubsante.hub.testsupport.MetricsUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import com.hubsante.hub.testsupport.HubTestScaffolding;
import com.hubsante.model.report.ErrorCode;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;

@DisplayName("Dispatcher — metrics")
class DispatcherMetricsTest {

    private Dispatcher dispatcher;
    private MeterRegistry registry;

    @BeforeEach
    void setUp() {
        HubTestScaffolding.Hub hub = aHub().build();
        dispatcher = hub.dispatcher();
        registry = hub.registry();
    }

    @Test
    @DisplayName("should increment counter")
    public void incrementMetricsCounter() throws IOException {
        // First we define specific searches to restrict metric vector on specific tags

        // total errors for samuA client (any reason, any vhost, etc)
        Search errorOverallSamuA = targetCounter(registry, CLIENT_ID_TAG, SAMU_A_ROUTING_KEY);
        // total contentType tagged errors for samuA client
        Search errorContentTypeSamuA =
                targetCounter(
                        registry,
                        REASON_TAG,
                        ErrorCode.NOT_ALLOWED_CONTENT_TYPE.getStatusString(),
                        CLIENT_ID_TAG,
                        SAMU_A_ROUTING_KEY);
        // total deliveryMode tagged errors for samuA client
        Search errorDeliveryModeSamuA =
                targetCounter(
                        registry,
                        REASON_TAG,
                        ErrorCode.DELIVERY_MODE_INCONSISTENCY.getStatusString(),
                        CLIENT_ID_TAG,
                        SAMU_A_ROUTING_KEY);
        // total deliveryMode tagged error for samu B client
        Search errorDeliveryModeSamuB =
                targetCounter(
                        registry,
                        REASON_TAG,
                        ErrorCode.DELIVERY_MODE_INCONSISTENCY.getStatusString(),
                        CLIENT_ID_TAG,
                        SAMU_B_ROUTING_KEY);

        // ensure counters are empty at startup
        assertNull(errorOverallSamuA.counter());
        assertNull(errorContentTypeSamuA.counter());
        assertNull(errorDeliveryModeSamuA.counter());

        // message without content type sent by SamuA
        Message noContentTypeMessageSamuA = createMessage("EDXL-DE", null);
        assertThrows(
                AmqpRejectAndDontRequeueException.class,
                () -> dispatcher.dispatch(noContentTypeMessageSamuA));

        // metrics filtered by "sender==samuA", or "sender==samuA &&
        // reason=NOT_ALLOWED_CONTENT_TYPE" should be 1
        // metrics filtered by "sender==samuA && reason==DELIVERY_MODE_INCONSISTENCY" should be 0
        assertEquals(1, getCurrentCount(errorContentTypeSamuA.counter()));
        assertNull(errorDeliveryModeSamuA.counter());
        assertEquals(1, getOverallCounterForClient(registry, SAMU_A_ROUTING_KEY));

        // same sender, different error
        Message nonPersistentMessageSamuA = createMessage("EDXL-DE", JSON);
        nonPersistentMessageSamuA
                .getMessageProperties()
                .setReceivedDeliveryMode(MessageDeliveryMode.NON_PERSISTENT);
        assertThrows(
                AmqpRejectAndDontRequeueException.class,
                () -> dispatcher.dispatch(nonPersistentMessageSamuA));

        // samuA && reason==NOT_ALLOWED_CONTENT_TYPE didn't increment
        // samuA && reason==DELIVERY_MODE_INCONSISTENCY is now 1
        // all errors for samuA is now 2
        assertEquals(1, getCurrentCount(errorContentTypeSamuA.counter()));
        assertEquals(1, getCurrentCount(errorDeliveryModeSamuA.counter()));
        assertEquals(2, getOverallCounterForClient(registry, SAMU_A_ROUTING_KEY));

        // create an DELIEVRY_MODE_INCONSISTENCY error, now from SamuB
        Message nonPersistentMessageSamuB = createMessage("EDXL-DE", XML, SAMU_B_ROUTING_KEY);
        nonPersistentMessageSamuB
                .getMessageProperties()
                .setReceivedDeliveryMode(MessageDeliveryMode.NON_PERSISTENT);
        assertThrows(
                AmqpRejectAndDontRequeueException.class,
                () -> dispatcher.dispatch(nonPersistentMessageSamuB));

        // samuB && reason==DELIVERY_MODE_INCONSISTENCY is now 1
        // overall reason==DELIVERY_MODE_INCONSISTENCY is now 2
        // overall editor=default-editor is now 3
        assertEquals(1, getCurrentCount(errorDeliveryModeSamuB.counter()));
        assertEquals(
                2,
                getOverallCounterForError(
                        registry, ErrorCode.DELIVERY_MODE_INCONSISTENCY.getStatusString()));
        assertEquals(3, getOverallCounterForEditor(registry, TEST_EDITOR));
    }
}
