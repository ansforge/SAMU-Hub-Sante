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
import static com.hubsante.hub.config.AmqpConfiguration.ORIGINAL_ROUTING_KEY;
import static com.hubsante.hub.config.Constants.*;
import static com.hubsante.hub.service.ConversionStubs.echoConversionService;
import static com.hubsante.hub.testsupport.HubTestConstants.*;
import static com.hubsante.hub.testsupport.HubTestScaffolding.aHub;
import static com.hubsante.hub.testsupport.MessageTestUtils.*;
import static com.hubsante.hub.testsupport.MetricsUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import com.hubsante.hub.testsupport.HubTestScaffolding;
import com.hubsante.model.report.ErrorCode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.search.Search;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@DisplayName("Dispatcher — metrics")
class DispatcherMetricsTest {

    private Dispatcher dispatcher;
    private MeterRegistry registry;
    private ConversionHandler conversionHandler;
    private ClientPropertiesRegistry clientPropertiesRegistry;
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void setUp() {
        HubTestScaffolding.Hub hub = aHub().build();
        dispatcher = hub.dispatcher();
        registry = hub.registry();
        conversionHandler = hub.conversionHandler();
        clientPropertiesRegistry = hub.clientPropertiesRegistry();
        rabbitTemplate = hub.rabbitTemplate();
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

    @Test
    @DisplayName("should publish DISPATCHED_MESSAGE once for a direct dispatch")
    public void shouldPublishDispatchedMessageForDirectRouting() throws IOException {
        Message message = createMessage("EDXL-DE", JSON);

        dispatcher.dispatch(message);

        assertEquals(1, dispatchedMessageCount(registry));
    }

    @Test
    @DisplayName("should not publish DISPATCHED_MESSAGE when conversion is needed")
    public void shouldNotPublishDispatchedMessageForAlreadyConversionMessages() throws IOException {
        echoConversionService(conversionHandler);
        Message message = createMessage("EDXL-DE", JSON, SAMU_A_ROUTING_KEY, SAMU_V1_ROUTING_KEY);
        message.getMessageProperties().setReceivedExchange("internal-transfer");

        dispatcher.dispatch(message);

        assertEquals(0, dispatchedMessageCount(registry));
    }

    @Test
    @DisplayName("should label a rejected message with its recipient and the routed use case")
    public void shouldLabelRejectedMessageWithRecipientAndUseCase() throws IOException {
        dispatcher.dispatch(createMessage("EDXL-DE", JSON));
        String useCase = registry.get(DISPATCHED_MESSAGE).counter().getId().getTag(USE_CASE_TAG);
        doReturn(List.of(useCase))
                .when(clientPropertiesRegistry)
                .getClientInhibitedUseCases(SAMU_B_ROUTING_KEY);

        assertThrows(
                AmqpRejectAndDontRequeueException.class,
                () -> dispatcher.dispatch(createMessage("EDXL-DE", JSON)));

        Counter error = registry.get(DISPATCH_ERROR).counter();
        assertEquals(SAMU_B_ROUTING_KEY, error.getId().getTag(RECIPIENT_ID_TAG));
        assertEquals(
                useCase,
                error.getId().getTag(USE_CASE_TAG),
                "use_case must have the same format in both metrics");
    }

    @Test
    @DisplayName("should label an invalid content with its recipient and an unknown use case")
    public void shouldLabelInvalidContentWithRecipientOnly() throws IOException {
        Message message =
                createInvalidMessage(
                        "EDXL-DE/invalid-content-valid-envelope.json", SAMU_A_ROUTING_KEY);

        assertThrows(AmqpRejectAndDontRequeueException.class, () -> dispatcher.dispatch(message));

        Counter error = registry.get(DISPATCH_ERROR).counter();
        assertEquals(SAMU_B_ROUTING_KEY, error.getId().getTag(RECIPIENT_ID_TAG));
        assertEquals(UNKNOWN, error.getId().getTag(USE_CASE_TAG));
    }

    @Test
    @DisplayName("should label an unreadable envelope as unknown")
    public void shouldLabelUnreadableEnvelopeUnknown() throws IOException {
        Message message =
                createInvalidMessage(
                        "EDXL-DE/missing-EDXL-required-field.json", SAMU_A_ROUTING_KEY);

        assertThrows(AmqpRejectAndDontRequeueException.class, () -> dispatcher.dispatch(message));

        Counter error = registry.get(DISPATCH_ERROR).counter();
        assertEquals(UNKNOWN, error.getId().getTag(RECIPIENT_ID_TAG));
        assertEquals(UNKNOWN, error.getId().getTag(USE_CASE_TAG));
    }

    @Test
    @DisplayName("should label a returned message with its recipient and use case")
    public void shouldLabelReturnedMessageWithRecipientAndUseCase() throws IOException {
        dispatcher.dispatch(createMessage("EDXL-DE", JSON));
        String useCase = registry.get(DISPATCHED_MESSAGE).counter().getId().getTag(USE_CASE_TAG);
        Message message = createMessage("EDXL-DE", JSON);
        message.getMessageProperties().setHeader(ORIGINAL_ROUTING_KEY, SAMU_A_ROUTING_KEY);

        returnsCallback().returnedMessage(returned(message));

        Counter error =
                registry.get(DISPATCH_ERROR)
                        .tag(REASON_TAG, ErrorCode.UNROUTABLE_MESSAGE.getStatusString())
                        .counter();
        assertEquals(SAMU_B_ROUTING_KEY, error.getId().getTag(RECIPIENT_ID_TAG));
        assertEquals(useCase, error.getId().getTag(USE_CASE_TAG));
    }

    @Test
    @DisplayName("should label an unreadable returned message as unknown")
    public void shouldLabelUnreadableReturnedMessageUnknown() {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(JSON);
        properties.setHeader(ORIGINAL_ROUTING_KEY, SAMU_A_ROUTING_KEY);
        Message message = new Message("not an EDXL".getBytes(StandardCharsets.UTF_8), properties);

        assertDoesNotThrow(() -> returnsCallback().returnedMessage(returned(message)));

        Counter error = registry.get(DISPATCH_ERROR).counter();
        assertEquals(UNKNOWN, error.getId().getTag(RECIPIENT_ID_TAG));
        assertEquals(UNKNOWN, error.getId().getTag(USE_CASE_TAG));
    }

    @Test
    @DisplayName("should publish business errors and returned messages with the same label keys")
    public void shouldPublishAllErrorSeriesWithTheSameLabelKeys() throws IOException {
        Message nonPersistent = createMessage("EDXL-DE", JSON);
        nonPersistent
                .getMessageProperties()
                .setReceivedDeliveryMode(MessageDeliveryMode.NON_PERSISTENT);
        assertThrows(
                AmqpRejectAndDontRequeueException.class, () -> dispatcher.dispatch(nonPersistent));
        Message unreadable =
                createInvalidMessage(
                        "EDXL-DE/missing-EDXL-required-field.json", SAMU_A_ROUTING_KEY);
        assertThrows(
                AmqpRejectAndDontRequeueException.class, () -> dispatcher.dispatch(unreadable));
        Message returned = createMessage("EDXL-DE", JSON);
        returned.getMessageProperties().setHeader(ORIGINAL_ROUTING_KEY, SAMU_A_ROUTING_KEY);
        returnsCallback().returnedMessage(returned(returned));

        // SimpleMeterRegistry accepts different key sets for one name, Prometheus does not
        assertEquals(3, registry.find(DISPATCH_ERROR).counters().size());
        assertEquals(
                Set.of(
                        Set.of(
                                REASON_TAG,
                                CLIENT_ID_TAG,
                                VHOST_TAG,
                                EDITOR_TAG,
                                RECIPIENT_ID_TAG,
                                USE_CASE_TAG)),
                tagKeySets(registry, DISPATCH_ERROR));
    }

    private RabbitTemplate.ReturnsCallback returnsCallback() {
        ArgumentCaptor<RabbitTemplate.ReturnsCallback> captor =
                ArgumentCaptor.forClass(RabbitTemplate.ReturnsCallback.class);
        verify(rabbitTemplate).setReturnsCallback(captor.capture());
        return captor.getValue();
    }

    private static ReturnedMessage returned(Message message) {
        return new ReturnedMessage(
                message, 312, "NO_ROUTE", DISTRIBUTION_EXCHANGE, SAMU_B_MESSAGE_QUEUE);
    }

    private static Set<Set<String>> tagKeySets(MeterRegistry registry, String name) {
        return registry.find(name).counters().stream()
                .map(
                        counter ->
                                counter.getId().getTags().stream()
                                        .map(Tag::getKey)
                                        .collect(Collectors.toSet()))
                .collect(Collectors.toSet());
    }

    private static double dispatchedMessageCount(MeterRegistry registry) {
        return registry.find(DISPATCHED_MESSAGE).counters().stream()
                .mapToDouble(Counter::count)
                .sum();
    }
}
