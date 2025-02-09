/**
 * Copyright © 2023-2025 Agence du Numerique en Sante (ANS)
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

import com.hubsante.hub.config.HubConfiguration;
import com.hubsante.hub.utils.ConversionUtils;
import com.hubsante.hub.utils.MessageUtils;
import com.hubsante.model.cisu.CreateCaseWrapper;
import com.hubsante.model.edxl.EdxlMessage;
import com.hubsante.model.emsi.EmsiWrapper;
import com.hubsante.model.health.CreateCaseHealthWrapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public class ConversionUtilsTest {

    @Mock
    private HubConfiguration hubConfig;

    @Mock
    private EdxlMessage edxlMessage;

    @Mock
    private CreateCaseWrapper createCaseWrapper;
    
    @Mock
    private CreateCaseHealthWrapper createCaseHealthWrapper;

    @Mock
    private EmsiWrapper emsiWrapper;

    private HashMap<String, Boolean> directCisuPreferences;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        directCisuPreferences = new HashMap<>();
        when(hubConfig.getDirectCisuPreferences()).thenReturn(directCisuPreferences);
    }

    @Test
    void testIsCisuExchange() {
        try (MockedStatic<MessageUtils> mockedMessageUtils = mockStatic(MessageUtils.class)) {
            // Health to Health (false)
            when(edxlMessage.getSenderID()).thenReturn("fr.health.samuA");
            mockedMessageUtils.when(() -> MessageUtils.getRecipientID(edxlMessage)).thenReturn("fr.health.samuB");
            assertFalse(ConversionUtils.isCisuExchange(edxlMessage));

            // Health to CISU (true)
            mockedMessageUtils.when(() -> MessageUtils.getRecipientID(edxlMessage)).thenReturn("fr.fire.sdisZ");
            assertTrue(ConversionUtils.isCisuExchange(edxlMessage));

            // CISU to Health (true)
            when(edxlMessage.getSenderID()).thenReturn("fr.fire.sdisZ");
            mockedMessageUtils.when(() -> MessageUtils.getRecipientID(edxlMessage)).thenReturn("fr.health.samuA");
            assertTrue(ConversionUtils.isCisuExchange(edxlMessage));
        }
    }

    @Test
    void testIsCisuModel() {
        // CreateCaseWrapper
        when(edxlMessage.getFirstContentMessage()).thenReturn(createCaseWrapper);
        assertTrue(ConversionUtils.isConvertedModel(edxlMessage));

        // CreateCaseHealthWrapper
        when(edxlMessage.getFirstContentMessage()).thenReturn(createCaseHealthWrapper);
        assertTrue(ConversionUtils.isConvertedModel(edxlMessage));

        // Other type (EMSI conversion not handled for now)
        when(edxlMessage.getFirstContentMessage()).thenReturn(emsiWrapper);
        assertFalse(ConversionUtils.isConvertedModel(edxlMessage));
    }

    @Test
    void testIsDirectCisuForHealthActor() {
        try (MockedStatic<MessageUtils> mockedMessageUtils = mockStatic(MessageUtils.class)) {
            when(edxlMessage.getSenderID()).thenReturn("fr.health.samuA");
            mockedMessageUtils.when(() -> MessageUtils.getRecipientID(edxlMessage)).thenReturn("fr.fire.sdisZ");

            // Health actor in direct CISU preferences - true
            directCisuPreferences.put("fr.health.samuA", true);
            assertTrue(ConversionUtils.isDirectCisuForHealthActor(hubConfig, edxlMessage));

            // Health actor in direct CISU preferences - false
            directCisuPreferences.put("fr.health.samuA", false);
            assertFalse(ConversionUtils.isDirectCisuForHealthActor(hubConfig, edxlMessage));

            // Health actor not in direct CISU preferences
            when(edxlMessage.getSenderID()).thenReturn("fr.health.samuB");
            assertFalse(ConversionUtils.isDirectCisuForHealthActor(hubConfig, edxlMessage));
        }
    }

    @Test
    void testRequiresCisuConversion() {
        try (MockedStatic<ConversionUtils> mockedConversionUtils = mockStatic(ConversionUtils.class)) {
            // List of test cases: isCisuExchange, isConvertedModel, isDirectCisuForHealthActor, expectedResult
            // samuA uses health models but samuB uses CISU models (is direct CISU)
            // EDA is a converted model
            // SNH = Should Not Happen
            List<Boolean[]> testCases = Arrays.asList(
                new Boolean[]{true,  true,  false, true},    // samuA -[RS-EDA]-> sdis => true  | sdis -[RC-EDA]-> samuA => true
                new Boolean[]{true,  true,  true,  false},   // samuB -[RC-EDA]-> sdis => false | sdis -[RC-EDA]-> samuB => false
                new Boolean[]{true,  false, false, false},    // Not CISU message => false
                new Boolean[]{true,  false, true,  false},   // Not CISU message => false
                new Boolean[]{false, true,  false, false},   // Not CISU exchange => false
                new Boolean[]{false, true,  true,  false},   // Not CISU exchange => false
                new Boolean[]{false, false, false, false},   // Not CISU exchange => false
                new Boolean[]{false, false, true,  false}    // Not CISU exchange => false
            );

            // Call the real method for requiresCisuConversion
            mockedConversionUtils.when(() -> ConversionUtils.requiresCisuConversion(hubConfig, edxlMessage))
                    .thenCallRealMethod();

            for (int i = 0; i < testCases.size(); i++) {
                Boolean[] testCase = testCases.get(i);
                
                // Mock the helper methods
                mockedConversionUtils.when(() -> ConversionUtils.isCisuExchange(edxlMessage)).thenReturn(testCase[0]);
                mockedConversionUtils.when(() -> ConversionUtils.isConvertedModel(edxlMessage)).thenReturn(testCase[1]);
                mockedConversionUtils.when(() -> ConversionUtils.isDirectCisuForHealthActor(hubConfig, edxlMessage)).thenReturn(testCase[2]);

                // Assert with descriptive message
                String testDescription = String.format(
                    "Test case %d failed: isCisuExchange=%b, isConvertedModel=%b, isDirectCisuForHealthActor=%b, expected=%b",
                    i, testCase[0], testCase[1], testCase[2], testCase[3]
                );
                assertEquals(testCase[3], ConversionUtils.requiresCisuConversion(hubConfig, edxlMessage), testDescription);
            }
        }
    }
}
