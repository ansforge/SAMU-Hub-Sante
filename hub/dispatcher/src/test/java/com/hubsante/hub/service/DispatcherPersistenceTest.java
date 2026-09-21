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

import static com.hubsante.hub.service.ConversionStubs.echoConversionService;
import static com.hubsante.hub.service.ConversionStubs.failConversionService;
import static com.hubsante.hub.service.ConversionStubs.verifyConversion;
import static com.hubsante.hub.service.ConversionStubs.verifyNoConversion;
import static com.hubsante.hub.testsupport.HubTestConstants.*;
import static com.hubsante.hub.testsupport.HubTestScaffolding.aHub;
import static com.hubsante.hub.testsupport.MessageTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.hubsante.hub.config.HubConfiguration;
import com.hubsante.hub.exception.HubPersistenceException;
import com.hubsante.hub.testsupport.HubTestScaffolding;
import com.hubsante.hub.utils.*;
import com.hubsante.model.edxl.EdxlMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;

@DisplayName("Dispatcher — persistence")
class DispatcherPersistenceTest {

    private Dispatcher dispatcher;
    private ConversionHandler conversionHandler;
    private MessagePersistenceService persistenceService;
    private HubConfiguration hubConfig;

    @BeforeEach
    void setUp() {
        HubTestScaffolding.Hub hub = aHub().build();
        dispatcher = hub.dispatcher();
        conversionHandler = hub.conversionHandler();
        persistenceService = hub.persistenceService();
        hubConfig = hub.hubConfig();
        echoConversionService(conversionHandler);
    }

    @Test
    @DisplayName("should call persistenceService before CISU conversion")
    public void shouldCallPersistenceServiceBeforeCisuConversion() throws Exception {
        try (MockedStatic<MessagePersistencePolicy> mockedPersistencePolicy =
                mockStatic(MessagePersistencePolicy.class)) {
            doReturn(NEXSIS_VHOST).when(hubConfig).getVhost();

            Message fromFireMessage =
                    createMessage("EDXL-DE", XML, SDIS_C_ROUTING_KEY, SAMU_V3_ROUTING_KEY);

            mockedPersistencePolicy
                    .when(() -> MessagePersistencePolicy.shouldPersist(anyString(), anyString()))
                    .thenReturn(true);

            dispatcher.dispatch(fromFireMessage);

            // Verify ordering: persistence must happen before conversion
            InOrder inOrder = inOrder(persistenceService, conversionHandler);
            inOrder.verify(persistenceService, times(1)).persist(any(EdxlMessage.class));
            verifyConversion(inOrder, conversionHandler);
        }
    }

    @Test
    @DisplayName("should not call persistenceService for version-only conversion (not CISU)")
    public void shouldNotCallPersistenceServiceForVersionConversion() throws Exception {
        Message message = createMessage("EDXL-DE", JSON, SAMU_A_ROUTING_KEY, SAMU_V1_ROUTING_KEY);

        dispatcher.dispatch(message);

        verify(persistenceService, never()).persist(any(EdxlMessage.class));
    }

    @Test
    @DisplayName("should have persisted message even if CISU conversion fails")
    public void shouldHavePersistedEvenIfCisuConversionFails() throws Exception {
        try (MockedStatic<MessagePersistencePolicy> mockedPersistencePolicy =
                mockStatic(MessagePersistencePolicy.class)) {
            doReturn(NEXSIS_VHOST).when(hubConfig).getVhost();

            Message fromFireMessage =
                    createMessage("EDXL-DE", XML, SDIS_C_ROUTING_KEY, SAMU_V3_ROUTING_KEY);

            mockedPersistencePolicy
                    .when(() -> MessagePersistencePolicy.shouldPersist(anyString(), anyString()))
                    .thenReturn(true);

            // Conversion failure: persistence should occur, conversion throws
            failConversionService(
                    conversionHandler, new RuntimeException("Conversion service unavailable"));

            // The conversion failure causes the dispatch to reject the message
            assertThrows(
                    AmqpRejectAndDontRequeueException.class,
                    () -> dispatcher.dispatch(fromFireMessage));

            // But persistence was already called before the conversion attempt
            verify(persistenceService, times(1)).persist(any(EdxlMessage.class));
        }
    }

    @Test
    @DisplayName("should wrap persistence exception in HubPersistenceException")
    public void shouldThrowPersistenceExceptionIfPersistenceFails() throws Exception {
        try (MockedStatic<MessagePersistencePolicy> mockedPersistencePolicy =
                mockStatic(MessagePersistencePolicy.class)) {
            doReturn(NEXSIS_VHOST).when(hubConfig).getVhost();

            Message fromFireMessage =
                    createMessage("EDXL-DE", XML, SDIS_C_ROUTING_KEY, SAMU_V3_ROUTING_KEY);

            mockedPersistencePolicy
                    .when(() -> MessagePersistencePolicy.shouldPersist(anyString(), anyString()))
                    .thenReturn(true);

            // persist() throws HubPersistenceException directly; Dispatcher lets it propagate to
            // handleError
            doThrow(new HubPersistenceException("Persistence failed", "distributionId"))
                    .when(persistenceService)
                    .persist(any(EdxlMessage.class));

            AmqpRejectAndDontRequeueException thrown =
                    assertThrows(
                            AmqpRejectAndDontRequeueException.class,
                            () -> dispatcher.dispatch(fromFireMessage));

            assertNotNull(thrown.getCause());
            assertInstanceOf(HubPersistenceException.class, thrown.getCause());
            assertTrue(thrown.getCause().getMessage().contains("Persistence failed"));
            // Conversion must not be called if persistence fails
            verifyNoConversion(conversionHandler);
        }
    }

    @Test
    @DisplayName("should not call persistenceService when no conversion is required")
    public void shouldNotCallPersistenceServiceForDirectDispatch() throws Exception {
        Message message = createMessage("EDXL-DE", JSON);
        dispatcher.dispatch(message);

        verify(persistenceService, never()).persist(any(EdxlMessage.class));
    }
}
