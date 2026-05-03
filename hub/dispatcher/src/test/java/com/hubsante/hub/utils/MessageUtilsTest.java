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

import static com.hubsante.hub.utils.MessageUtils.checkMessageNotInhibited;
import static org.junit.jupiter.api.Assertions.*;

import com.hubsante.hub.exception.UnroutableMessageException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class MessageUtilsTest {

    private final String UNRESTRICTED_CLIENT = "fr.health.unrestricted_client";
    private final String LIMITED_CLIENT = "fr.health.limited_client";
    private final String INHIBITED_MESSAGE = "ResourcesInfoCisuWrapper";
    private final String UNRESTRICTED_MESSAGE = "CreateCaseWrapper";
    private final String DISTRIBUTION_ID = "some_distribution_id";

    Map<String, List<String>> inhibitedMessagesByClient =
            Map.of(
                    UNRESTRICTED_CLIENT, List.of(),
                    LIMITED_CLIENT, List.of(INHIBITED_MESSAGE));

    @Test
    @DisplayName("should throw if message is inhibited for restricted client")
    public void shouldThrowIfMessageIsInhibited() {
        UnroutableMessageException thrown = assertThrows(
                UnroutableMessageException.class,
                () ->
                        checkMessageNotInhibited(
                                LIMITED_CLIENT,
                                INHIBITED_MESSAGE,
                                inhibitedMessagesByClient,
                                DISTRIBUTION_ID));

        assertEquals(
                "Use case " + INHIBITED_MESSAGE + " is not supported for client " + LIMITED_CLIENT,
                thrown.getMessage());
    }

    @Test
    @DisplayName("should not throw if message is not inhibited for restricted client")
    public void shouldNotThrowIfMessageIsNotInhibited() {
        assertDoesNotThrow(
                () ->
                        checkMessageNotInhibited(
                                LIMITED_CLIENT,
                                UNRESTRICTED_MESSAGE,
                                inhibitedMessagesByClient,
                                DISTRIBUTION_ID));
    }

    @Test
    @DisplayName("should not throw id client has no restrictions")
    public void shouldNotThrowIfIdClientHasNoRestrictions() {
        assertDoesNotThrow(
                () ->
                        checkMessageNotInhibited(
                                UNRESTRICTED_CLIENT,
                                INHIBITED_MESSAGE,
                                inhibitedMessagesByClient,
                                DISTRIBUTION_ID));
    }
}
