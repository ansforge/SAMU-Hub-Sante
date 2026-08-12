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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.hubsante.hub.config.HubConfiguration;
import com.hubsante.hub.exception.UnroutableMessageException;
import com.hubsante.hub.utils.EdxlUtils;
import com.hubsante.model.EdxlHandler;
import com.hubsante.model.Validator;
import com.hubsante.model.edxl.ContentMessage;
import com.hubsante.model.edxl.Descriptor;
import com.hubsante.model.edxl.EdxlMessage;
import com.hubsante.model.edxl.ExplicitAddress;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class MessageHandlerTest {

    private static final String UNRESTRICTED_CLIENT = "fr.health.unrestricted_client";
    private static final String LIMITED_CLIENT = "fr.health.limited_client";
    private static final String INHIBITED_MESSAGE = "ResourcesInfoCisuWrapper";
    private static final String UNRESTRICTED_MESSAGE = "CreateCaseWrapper";
    private static final String DISTRIBUTION_ID = "some_distribution_id";

    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private EdxlHandler edxlHandler;
    @Mock private HubConfiguration hubConfiguration;
    @Mock private ClientPropertiesRegistry clientPropertiesRegistry;
    @Mock private Validator validator;
    @Mock private MeterRegistry meterRegistry;
    @Mock private XmlMapper xmlMapper;
    @Mock private ObjectMapper jsonMapper;
    @Mock private ConversionHandler conversionHandler;
    @Mock private EdxlMessage edxlMessage;
    @Mock private ContentMessage contentMessage;

    @Mock private Descriptor descriptor;
    @Mock private ExplicitAddress explicitAddress;

    private MessageHandler messageHandler;

    @BeforeEach
    void setUp() {
        messageHandler =
                spy(
                        new MessageHandler(
                                rabbitTemplate,
                                edxlHandler,
                                hubConfiguration,
                                validator,
                                meterRegistry,
                                xmlMapper,
                                jsonMapper,
                                conversionHandler));

        when(hubConfiguration.getClientPropertiesRegistry()).thenReturn(clientPropertiesRegistry);

        when(edxlMessage.getFirstContentMessage()).thenReturn(contentMessage);
        when(edxlMessage.getDescriptor()).thenReturn(descriptor);
        when(descriptor.getExplicitAddress()).thenReturn(explicitAddress);
    }

    @Test
    @DisplayName("should throw if message is inhibited for restricted client")
    void shouldThrowIfMessageIsInhibited() {

        when(edxlMessage.getDistributionID()).thenReturn(DISTRIBUTION_ID);
        when(explicitAddress.getExplicitAddressValue()).thenReturn(LIMITED_CLIENT);
        when(clientPropertiesRegistry.getClientInhibitedUseCases(LIMITED_CLIENT))
                .thenReturn(List.of(INHIBITED_MESSAGE));

        try (MockedStatic<EdxlUtils> mockedStatic = mockStatic(EdxlUtils.class)) {
            mockedStatic
                    .when(() -> EdxlUtils.getUseCaseFromMessage(contentMessage))
                    .thenReturn(INHIBITED_MESSAGE);

            UnroutableMessageException thrown =
                    assertThrows(
                            UnroutableMessageException.class,
                            () -> messageHandler.inhibitMessageIfNeeded(edxlMessage));

            assertEquals(
                    "Use case "
                            + INHIBITED_MESSAGE
                            + " is not supported for client "
                            + LIMITED_CLIENT,
                    thrown.getMessage());
        }
    }

    @Test
    @DisplayName("should not throw if message is not inhibited for restricted client")
    void shouldNotThrowIfMessageIsNotInhibited() {

        when(explicitAddress.getExplicitAddressValue()).thenReturn(LIMITED_CLIENT);
        when(clientPropertiesRegistry.getClientInhibitedUseCases(LIMITED_CLIENT))
                .thenReturn(List.of(INHIBITED_MESSAGE));

        try (MockedStatic<EdxlUtils> mockedStatic = mockStatic(EdxlUtils.class)) {
            mockedStatic
                    .when(() -> EdxlUtils.getUseCaseFromMessage(contentMessage))
                    .thenReturn(UNRESTRICTED_MESSAGE);

            assertDoesNotThrow(() -> messageHandler.inhibitMessageIfNeeded(edxlMessage));
        }
    }

    @Test
    @DisplayName("should not throw if client has no restrictions")
    void shouldNotThrowIfClientHasNoRestrictions() {

        when(explicitAddress.getExplicitAddressValue()).thenReturn(UNRESTRICTED_CLIENT);
        when(clientPropertiesRegistry.getClientInhibitedUseCases(UNRESTRICTED_CLIENT))
                .thenReturn(List.of());

        try (MockedStatic<EdxlUtils> mockedStatic = mockStatic(EdxlUtils.class)) {
            mockedStatic
                    .when(() -> EdxlUtils.getUseCaseFromMessage(contentMessage))
                    .thenReturn(INHIBITED_MESSAGE);

            assertDoesNotThrow(() -> messageHandler.inhibitMessageIfNeeded(edxlMessage));
        }
    }
}
