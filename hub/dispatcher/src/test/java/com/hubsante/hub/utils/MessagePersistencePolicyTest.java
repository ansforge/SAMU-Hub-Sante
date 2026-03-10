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
package com.hubsante.hub.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class MessagePersistencePolicyTest {

    // ─── Nexsis vhost (18 → 15) ───────────────────────────────────────────────

    @Test
    @DisplayName("should persist ResourcesInfoCisuWrapper (RC-RI) when vhost is 15-nexsis")
    void shouldPersistResourcesInfoCisuWrapperFromNexsisVhost() {
        assertTrue(
                MessagePersistencePolicy.shouldPersist(
                        "15-nexsis_v1.9", "ResourcesInfoCisuWrapper"));
    }

    @ParameterizedTest(name = "should not persist {0} when vhost is 15-nexsis")
    @ValueSource(
            strings = {
                "CreateCaseWrapper", // RC-EDA
                "CreateCaseHealthWrapper", // RS-EDA
                "ResourcesInfoWrapper", // RS-RI (health type, wrong direction)
                "ResourcesStatusWrapper", // RS-SR (health type, wrong direction)
                "TechnicalNoreqWrapper",
                "ReferenceWrapper",
                "ErrorWrapper"
            })
    @DisplayName("should not persist non-RC-RI types when vhost is 15-nexsis")
    void shouldNotPersistNonAllowedTypeFromNexsisVhost(String useCase) {
        assertFalse(MessagePersistencePolicy.shouldPersist("15-nexsis_v1.9", useCase));
    }

    // ─── Health vhost (15 → 18) ───────────────────────────────────────────────

    @Test
    @DisplayName("should persist ResourcesInfoWrapper (RS-RI) when vhost is 15-15_v*")
    void shouldPersistResourcesInfoWrapperFromHealthVhost() {
        assertTrue(MessagePersistencePolicy.shouldPersist("15-15_v2.1", "ResourcesInfoWrapper"));
    }

    @Test
    @DisplayName("should persist ResourcesStatusWrapper (RS-SR) when vhost is 15-15_v*")
    void shouldPersistResourcesStatusWrapperFromHealthVhost() {
        assertTrue(MessagePersistencePolicy.shouldPersist("15-15_v1.5", "ResourcesStatusWrapper"));
    }

    @ParameterizedTest(name = "should not persist {0} when vhost is 15-15_v*")
    @ValueSource(
            strings = {
                "CreateCaseWrapper", // RC-EDA
                "CreateCaseHealthWrapper", // RS-EDA
                "ResourcesInfoCisuWrapper", // RC-RI (nexsis type, wrong direction)
                "TechnicalNoreqWrapper",
                "ReferenceWrapper",
                "ErrorWrapper"
            })
    @DisplayName("should not persist non-RS-RI/RS-SR types when vhost is 15-15_v*")
    void shouldNotPersistNonAllowedTypeFromHealthVhost(String useCase) {
        assertFalse(MessagePersistencePolicy.shouldPersist("15-15_v2.1", useCase));
    }

    @Test
    @DisplayName("should persist ResourcesInfoWrapper on any 15-15_v* version")
    void shouldPersistOnAnyHealthVhostVersion() {
        assertTrue(MessagePersistencePolicy.shouldPersist("15-15_v1.5", "ResourcesInfoWrapper"));
        assertTrue(MessagePersistencePolicy.shouldPersist("15-15_v2.0", "ResourcesInfoWrapper"));
        assertTrue(MessagePersistencePolicy.shouldPersist("15-15_v2.1", "ResourcesStatusWrapper"));
    }

    // ─── Unknown / null vhost ─────────────────────────────────────────────────

    @Test
    @DisplayName("should not persist any message when vhost is unknown")
    void shouldNotPersistFromUnknownVhost() {
        assertFalse(
                MessagePersistencePolicy.shouldPersist(
                        "some-other-vhost", "ResourcesInfoCisuWrapper"));
        assertFalse(
                MessagePersistencePolicy.shouldPersist("some-other-vhost", "ResourcesInfoWrapper"));
    }

    @Test
    @DisplayName("should not persist when vhost is null")
    void shouldNotPersistWhenVhostIsNull() {
        assertFalse(MessagePersistencePolicy.shouldPersist(null, "ResourcesInfoCisuWrapper"));
        assertFalse(MessagePersistencePolicy.shouldPersist(null, "ResourcesInfoWrapper"));
    }
}
