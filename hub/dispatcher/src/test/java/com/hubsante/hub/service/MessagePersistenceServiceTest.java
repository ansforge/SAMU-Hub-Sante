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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubsante.hub.repository.MessageRepository;
import com.hubsante.hub.utils.EdxlUtils;
import com.hubsante.model.EdxlHandler;
import com.hubsante.model.edxl.ContentMessage;
import com.hubsante.model.edxl.EdxlMessage;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MessagePersistenceServiceTest {

    @Mock private MessageRepository repository;
    @Mock private EdxlHandler edxlHandler;
    @Mock private ObjectMapper objectMapper;
    @Mock private EdxlMessage edxlMessage;
    @Mock private ContentMessage contentMessage;

    private MessagePersistenceService service;

    @BeforeEach
    void setUp() {
        service = new MessagePersistenceService(repository, edxlHandler, objectMapper);
        // These stubs are not used in "should not persist" tests (early return), so lenient
        lenient().when(edxlMessage.getFirstContentMessage()).thenReturn(contentMessage);
        lenient().when(edxlMessage.getDistributionID()).thenReturn("test-distribution-id");
    }

    // ─── Nexsis vhost (18 → 15) ───────────────────────────────────────────────

    @Test
    @DisplayName("should persist ResourcesInfoCisuWrapper when vhost is 15-nexsis")
    void shouldPersistResourcesInfoCisuWrapperFromNexsisVhost() throws Exception {
        try (MockedStatic<EdxlUtils> mockedEdxlUtils = mockStatic(EdxlUtils.class)) {
            mockedEdxlUtils
                    .when(() -> EdxlUtils.getUseCaseFromMessage(any()))
                    .thenReturn("ResourcesInfoCisuWrapper");
            when(edxlHandler.serializeJsonEDXL(any())).thenReturn("{\"distributionID\":\"test\"}");
            when(objectMapper.readValue(anyString(), any(TypeReference.class)))
                    .thenReturn(Map.of());

            service.persistIfRequired(edxlMessage, "15-nexsis_v1.9");

            verify(repository, times(1)).save(any(com.hubsante.hub.model.Message.class));
        }
    }

    @ParameterizedTest(name = "should not persist {0} when vhost is 15-nexsis")
    @ValueSource(strings = {
        "CreateCaseWrapper",        // RC-EDA
        "CreateCaseHealthWrapper",  // RS-EDA
        "ResourcesInfoWrapper",     // RS-RI (health type, wrong direction)
        "ResourcesStatusWrapper",   // RS-SR (health type, wrong direction)
        "TechnicalNoreqWrapper",
        "ReferenceWrapper",
        "ErrorWrapper"
    })
    @DisplayName("should not persist non-RC-RI types when vhost is 15-nexsis")
    void shouldNotPersistNonAllowedTypeFromNexsisVhost(String useCase) {
        try (MockedStatic<EdxlUtils> mockedEdxlUtils = mockStatic(EdxlUtils.class)) {
            mockedEdxlUtils
                    .when(() -> EdxlUtils.getUseCaseFromMessage(any()))
                    .thenReturn(useCase);

            service.persistIfRequired(edxlMessage, "15-nexsis_v1.9");

            verify(repository, never()).save(any());
        }
    }

    // ─── Health vhost (15 → 18) ───────────────────────────────────────────────

    @Test
    @DisplayName("should persist ResourcesInfoWrapper when vhost is 15-15_v*")
    void shouldPersistResourcesInfoWrapperFromHealthVhost() throws Exception {
        try (MockedStatic<EdxlUtils> mockedEdxlUtils = mockStatic(EdxlUtils.class)) {
            mockedEdxlUtils
                    .when(() -> EdxlUtils.getUseCaseFromMessage(any()))
                    .thenReturn("ResourcesInfoWrapper");
            when(edxlHandler.serializeJsonEDXL(any())).thenReturn("{\"distributionID\":\"test\"}");
            when(objectMapper.readValue(anyString(), any(TypeReference.class)))
                    .thenReturn(Map.of());

            service.persistIfRequired(edxlMessage, "15-15_v2.1");

            verify(repository, times(1)).save(any(com.hubsante.hub.model.Message.class));
        }
    }

    @Test
    @DisplayName("should persist ResourcesStatusWrapper when vhost is 15-15_v*")
    void shouldPersistResourcesStatusWrapperFromHealthVhost() throws Exception {
        try (MockedStatic<EdxlUtils> mockedEdxlUtils = mockStatic(EdxlUtils.class)) {
            mockedEdxlUtils
                    .when(() -> EdxlUtils.getUseCaseFromMessage(any()))
                    .thenReturn("ResourcesStatusWrapper");
            when(edxlHandler.serializeJsonEDXL(any())).thenReturn("{\"distributionID\":\"test\"}");
            when(objectMapper.readValue(anyString(), any(TypeReference.class)))
                    .thenReturn(Map.of());

            service.persistIfRequired(edxlMessage, "15-15_v1.5");

            verify(repository, times(1)).save(any(com.hubsante.hub.model.Message.class));
        }
    }

    @ParameterizedTest(name = "should not persist {0} when vhost is 15-15_v*")
    @ValueSource(strings = {
        "CreateCaseWrapper",          // RC-EDA
        "CreateCaseHealthWrapper",    // RS-EDA
        "ResourcesInfoCisuWrapper",   // RC-RI (nexsis type, wrong direction)
        "TechnicalNoreqWrapper",
        "ReferenceWrapper",
        "ErrorWrapper"
    })
    @DisplayName("should not persist non-RS-RI/RS-SR types when vhost is 15-15_v*")
    void shouldNotPersistNonAllowedTypeFromHealthVhost(String useCase) {
        try (MockedStatic<EdxlUtils> mockedEdxlUtils = mockStatic(EdxlUtils.class)) {
            mockedEdxlUtils
                    .when(() -> EdxlUtils.getUseCaseFromMessage(any()))
                    .thenReturn(useCase);

            service.persistIfRequired(edxlMessage, "15-15_v2.1");

            verify(repository, never()).save(any());
        }
    }

    // ─── Unknown vhost ────────────────────────────────────────────────────────

    @Test
    @DisplayName("should not persist any message when vhost is unknown")
    void shouldNotPersistFromUnknownVhost() {
        try (MockedStatic<EdxlUtils> mockedEdxlUtils = mockStatic(EdxlUtils.class)) {
            mockedEdxlUtils
                    .when(() -> EdxlUtils.getUseCaseFromMessage(any()))
                    .thenReturn("ResourcesInfoCisuWrapper");

            service.persistIfRequired(edxlMessage, "some-other-vhost");

            verify(repository, never()).save(any());
        }
    }

    @Test
    @DisplayName("should not persist when vhost is null")
    void shouldNotPersistWhenVhostIsNull() {
        try (MockedStatic<EdxlUtils> mockedEdxlUtils = mockStatic(EdxlUtils.class)) {
            mockedEdxlUtils
                    .when(() -> EdxlUtils.getUseCaseFromMessage(any()))
                    .thenReturn("ResourcesInfoCisuWrapper");

            service.persistIfRequired(edxlMessage, null);

            verify(repository, never()).save(any());
        }
    }
}
