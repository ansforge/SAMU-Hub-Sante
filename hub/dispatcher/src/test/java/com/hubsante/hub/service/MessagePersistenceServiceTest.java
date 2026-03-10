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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubsante.hub.config.HubConfiguration;
import com.hubsante.hub.model.PersistedMessage;
import com.hubsante.hub.repository.PersistedMessageRepository;
import com.hubsante.hub.utils.EdxlUtils;
import com.hubsante.model.EdxlHandler;
import com.hubsante.model.edxl.ContentMessage;
import com.hubsante.model.edxl.EdxlMessage;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MessagePersistenceServiceTest {

    @Mock private PersistedMessageRepository repository;
    @Mock private EdxlHandler edxlHandler;
    @Mock private ObjectMapper objectMapper;
    @Mock private EdxlMessage edxlMessage;
    @Mock private ContentMessage contentMessage;

    private MessagePersistenceService service;
    @Mock private HubConfiguration hubConfig;

    @BeforeEach
    void setUp() {
        service = new MessagePersistenceService(repository, edxlHandler, objectMapper, hubConfig);
        // These stubs are not used in all tests, so lenient
        lenient().when(edxlMessage.getFirstContentMessage()).thenReturn(contentMessage);
        lenient().when(edxlMessage.getDistributionID()).thenReturn("test-distribution-id");
    }

    // ─── Nexsis vhost (18 → 15) ───────────────────────────────────────────────

    @Test
    @DisplayName("should persist ResourcesInfoCisuWrapper when vhost is 15-nexsis")
    void shouldPersistResourcesInfoCisuWrapperFromNexsisVhost() throws Exception {
        when(hubConfig.getVhost()).thenReturn("15-nexsis_v1.9");
        try (MockedStatic<EdxlUtils> mockedEdxlUtils = mockStatic(EdxlUtils.class)) {
            mockedEdxlUtils
                    .when(() -> EdxlUtils.getUseCaseFromMessage(any()))
                    .thenReturn("ResourcesInfoCisuWrapper");
            when(edxlHandler.serializeJsonEDXL(any())).thenReturn("{\"distributionID\":\"test\"}");
            when(objectMapper.readValue(anyString(), any(TypeReference.class)))
                    .thenReturn(Map.of());

            service.persist(edxlMessage);

            verify(repository, times(1)).save(any(PersistedMessage.class));
        }
    }

    // ─── Health vhost (15 → 18) ───────────────────────────────────────────────

    @Test
    @DisplayName("should persist ResourcesInfoWrapper when vhost is 15-15_v*")
    void shouldPersistResourcesInfoWrapperFromHealthVhost() throws Exception {
        when(hubConfig.getVhost()).thenReturn("15-15_v2.1");
        try (MockedStatic<EdxlUtils> mockedEdxlUtils = mockStatic(EdxlUtils.class)) {
            mockedEdxlUtils
                    .when(() -> EdxlUtils.getUseCaseFromMessage(any()))
                    .thenReturn("ResourcesInfoWrapper");
            when(edxlHandler.serializeJsonEDXL(any())).thenReturn("{\"distributionID\":\"test\"}");
            when(objectMapper.readValue(anyString(), any(TypeReference.class)))
                    .thenReturn(Map.of());

            service.persist(edxlMessage);

            verify(repository, times(1)).save(any(PersistedMessage.class));
        }
    }

    @Test
    @DisplayName("should persist ResourcesStatusWrapper when vhost is 15-15_v*")
    void shouldPersistResourcesStatusWrapperFromHealthVhost() throws Exception {
        when(hubConfig.getVhost()).thenReturn("15-15_v1.5");
        try (MockedStatic<EdxlUtils> mockedEdxlUtils = mockStatic(EdxlUtils.class)) {
            mockedEdxlUtils
                    .when(() -> EdxlUtils.getUseCaseFromMessage(any()))
                    .thenReturn("ResourcesStatusWrapper");
            when(edxlHandler.serializeJsonEDXL(any())).thenReturn("{\"distributionID\":\"test\"}");
            when(objectMapper.readValue(anyString(), any(TypeReference.class)))
                    .thenReturn(Map.of());

            service.persist(edxlMessage);

            verify(repository, times(1)).save(any(PersistedMessage.class));
        }
    }

    // ─── Error resilience ─────────────────────────────────────────────────────

    @Test
    @DisplayName("should throw when repository.save() fails")
    void shouldThrowWhenRepositoryFails() throws Exception {
        when(hubConfig.getVhost()).thenReturn("15-nexsis_v1.9");
        try (MockedStatic<EdxlUtils> mockedEdxlUtils = mockStatic(EdxlUtils.class)) {
            mockedEdxlUtils
                    .when(() -> EdxlUtils.getUseCaseFromMessage(any()))
                    .thenReturn("ResourcesInfoCisuWrapper");
            when(edxlHandler.serializeJsonEDXL(any())).thenReturn("{\"distributionID\":\"test\"}");
            when(objectMapper.readValue(anyString(), any(TypeReference.class)))
                    .thenReturn(Map.of());
            doThrow(new RuntimeException("MongoDB unavailable")).when(repository).save(any());

            assertThrows(RuntimeException.class, () -> service.persist(edxlMessage));

            // save() was attempted but failed
            verify(repository, times(1)).save(any());
        }
    }

    @Test
    @DisplayName("should throw when serialization fails - save is never attempted")
    void shouldThrowWhenSerializationFails() throws Exception {
        when(hubConfig.getVhost()).thenReturn("15-15_v2.1");
        try (MockedStatic<EdxlUtils> mockedEdxlUtils = mockStatic(EdxlUtils.class)) {
            mockedEdxlUtils
                    .when(() -> EdxlUtils.getUseCaseFromMessage(any()))
                    .thenReturn("ResourcesInfoWrapper");
            when(edxlHandler.serializeJsonEDXL(any()))
                    .thenThrow(new RuntimeException("Serialization error"));

            assertThrows(RuntimeException.class, () -> service.persist(edxlMessage));

            // save() was never called because serialization failed before reaching it
            verify(repository, never()).save(any());
        }
    }
}
