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

import static com.hubsante.hub.utils.ConversionUtils.trimVersionSuffix;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.hubsante.hub.config.HubConfiguration;
import com.hubsante.hub.utils.ConversionUtils;
import com.hubsante.model.cisu.CreateCaseWrapper;
import com.hubsante.model.edxl.EdxlMessage;
import com.hubsante.model.emsi.EmsiWrapper;
import com.hubsante.model.health.CreateCaseHealthWrapper;
import java.util.HashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ConversionUtilsTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private HubConfiguration hubConfig;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private EdxlMessage edxlMessage;

    @Mock private CreateCaseWrapper createCaseWrapper;

    @Mock private CreateCaseHealthWrapper createCaseHealthWrapper;

    @Mock private EmsiWrapper emsiWrapper;

    private HashMap<String, Boolean> directCisuPreferences;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        directCisuPreferences = new HashMap<>();
        directCisuPreferences.put("fr.health.samuDirectCisu", true);

        when(hubConfig.getDirectCisuPreferences()).thenReturn(directCisuPreferences);
    }

    @Test
    void testBuildExchange() {
        assertEquals(
                "transfer_15-15_v1.5_to_15-15_v2.0",
                ConversionUtils.buildExchangeDestination("15-15_v1.5", "15-15_v2.0"));
        assertEquals(
                "transfer_toto_to_titi", ConversionUtils.buildExchangeDestination("toto", "titi"));
    }

    @Test
    public void testTrimVersionSuffix() {
        assertEquals("15-15", trimVersionSuffix("15-15_v1.3"));
        assertEquals("15-nexsis", trimVersionSuffix("15-nexsis_v2"));
        assertEquals("backup", trimVersionSuffix("backup_v2.0.1"));
        assertEquals("no-version-here", trimVersionSuffix("no-version-here"));
        assertNull(trimVersionSuffix(null));
        assertEquals("", trimVersionSuffix(""));
    }

    @Test
    void testFormatVersionToVhosts() {
        String[] versions = {"1.5", "2.0"};
        String[] expected = {"15-15_v1.5", "15-15_v2.0"};
        assertArrayEquals(
                expected, ConversionUtils.formatPerimeterVersionListToVhosts(versions, "15-15"));

        versions = new String[] {"1.5", "2.0"};
        expected = new String[] {"15-smur_v1.5", "15-smur_v2.0"};
        assertArrayEquals(
                expected, ConversionUtils.formatPerimeterVersionListToVhosts(versions, "15-smur"));

        String[] empty = {};
        String[] expectedEmpty = {};
        assertArrayEquals(
                expectedEmpty, ConversionUtils.formatPerimeterVersionListToVhosts(empty, "15-15"));

        assertNull(ConversionUtils.formatPerimeterVersionListToVhosts(null, "15-15"));
    }
}
