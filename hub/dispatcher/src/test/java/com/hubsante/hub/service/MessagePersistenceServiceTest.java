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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubsante.hub.config.HubConfiguration;
import com.hubsante.hub.exception.HubPersistenceException;
import com.hubsante.hub.model.PersistedMessage;
import com.hubsante.hub.repository.PersistedMessageRepository;
import com.hubsante.model.EdxlHandler;
import com.hubsante.model.edxl.EdxlMessage;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MessagePersistenceServiceTest {

    private static final String DISTRIBUTION_ID = "fr.health.samuA_test-distribution-id";
    private static final String SERIALIZED_EDXL = "{\"distributionID\":\"test\"}";

    /** A real handler, used only to build the fixtures; the service gets the mock below. */
    private static final EdxlHandler EDXL = new EdxlHandler();

    @Mock private PersistedMessageRepository repository;
    @Mock private EdxlHandler edxlHandler;
    @Mock private ObjectMapper objectMapper;
    @Mock private HubConfiguration hubConfig;

    private MessagePersistenceService service;

    @BeforeEach
    void setUp() {
        service = new MessagePersistenceService(repository, edxlHandler, objectMapper, hubConfig);
    }

    @ParameterizedTest(name = "{1} on vhost {0} is persisted as {2}")
    @CsvSource({
        "15-nexsis_vactive, resourcesInfoCisu, ResourcesInfoCisuWrapper",
        "15-15_v2.1,        resourcesInfo,     ResourcesInfoWrapper",
        "15-15_v1.5,        resourcesStatus,   ResourcesStatusWrapper",
    })
    @DisplayName("should persist the message under its use case name")
    void shouldPersistAllowedUseCase(String vhost, String contentKey, String expectedUseCase)
            throws Exception {
        when(hubConfig.getVhost()).thenReturn(vhost);
        when(edxlHandler.serializeJsonEDXL(any())).thenReturn(SERIALIZED_EDXL);
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(Map.of());

        service.persist(messageCarrying(contentKey));

        ArgumentCaptor<PersistedMessage> saved = ArgumentCaptor.forClass(PersistedMessage.class);
        verify(repository, times(1)).save(saved.capture());
        assertThat(saved.getValue().getType()).as("persisted use case").isEqualTo(expectedUseCase);
    }

    @Test
    @DisplayName("should throw when repository.save() fails")
    void shouldThrowWhenRepositoryFails() throws Exception {
        when(hubConfig.getVhost()).thenReturn("15-nexsis_v1.9");
        when(edxlHandler.serializeJsonEDXL(any())).thenReturn(SERIALIZED_EDXL);
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(Map.of());
        doThrow(new RuntimeException("MongoDB unavailable")).when(repository).save(any());

        assertThatThrownBy(() -> service.persist(messageCarrying("resourcesInfoCisu")))
                .isInstanceOf(HubPersistenceException.class)
                .hasMessageContaining("MongoDB unavailable");

        // save() was attempted but failed
        verify(repository, times(1)).save(any());
    }

    @Test
    @DisplayName("should throw when serialization fails - save is never attempted")
    void shouldThrowWhenSerializationFails() throws Exception {
        when(hubConfig.getVhost()).thenReturn("15-15_v2.1");
        when(edxlHandler.serializeJsonEDXL(any()))
                .thenThrow(new RuntimeException("Serialization error"));

        assertThatThrownBy(() -> service.persist(messageCarrying("resourcesInfo")))
                .isInstanceOf(HubPersistenceException.class)
                .hasMessageContaining("Serialization error");

        // save() was never called because serialization failed before reaching it
        verify(repository, never()).save(any());
    }

    /**
     * Builds the EDXL from JSON rather than from the model classes, so the fixture reads as data and
     * keeps compiling across the model-library versions of the CI matrix. {@code contentKey} is the
     * wrapper's JSON property, which is what determines the persisted use case.
     */
    private static EdxlMessage messageCarrying(String contentKey) {
        String json =
                """
                {
                  "distributionID": "%s",
                  "senderID": "fr.health.samuA",
                  "dateTimeSent": "2022-07-25T10:04:34+01:00",
                  "dateTimeExpires": "2072-07-25T10:04:34+01:00",
                  "distributionStatus": "Actual",
                  "distributionKind": "Report",
                  "descriptor": {
                    "language": "fr-FR",
                    "explicitAddress": {
                      "explicitAddressScheme": "hubex",
                      "explicitAddressValue": "fr.health.samuB"
                    }
                  },
                  "content": [
                    { "jsonContent": { "embeddedJsonContent": { "message": { "%s": {} } } } }
                  ]
                }
                """
                        .formatted(DISTRIBUTION_ID, contentKey);
        try {
            return EDXL.deserializeJsonEDXL(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not build the test EDXL message", e);
        }
    }
}
