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
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static com.hubsante.hub.utils.ConversionUtils.isTargetPerimeter;
import static com.hubsante.hub.utils.ConversionUtils.isTranscodingVersion;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public class ConversionUtilsTest {

    @Mock (answer = Answers.RETURNS_DEEP_STUBS)
    private HubConfiguration hubConfig;

    @Mock (answer = Answers.RETURNS_DEEP_STUBS)
    private EdxlMessage edxlMessage;

    @Mock
    private CreateCaseWrapper createCaseWrapper;

    @Mock
    private CreateCaseHealthWrapper createCaseHealthWrapper;

    @Mock
    private EmsiWrapper emsiWrapper;

    private HashMap<String, Boolean> directCisuPreferences;

    private HashMap<String, String[]> lrmPerimeterVersions;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        directCisuPreferences = new HashMap<>();
        when(hubConfig.getDirectCisuPreferences()).thenReturn(directCisuPreferences);

        lrmPerimeterVersions = new HashMap<>();
        lrmPerimeterVersions.put("fr.health.samuA", new String[]{"1.5", "2.0", "2.1"});
        lrmPerimeterVersions.put("fr.health.samuV1", new String[]{"1.5"});
        lrmPerimeterVersions.put("fr.health.samuV2", new String[]{"2.0"});
        lrmPerimeterVersions.put("fr.health.samuEmpty", new String[]{});
        lrmPerimeterVersions.put("fr.health.samuNull", null);
        when(hubConfig.getLrmPerimeterVersions()).thenReturn(lrmPerimeterVersions);
    }

    @Test
    void testRequiresConversion(){
        try (MockedStatic<ConversionUtils> mockedConversionUtils = mockStatic(ConversionUtils.class)) {
            // List of test cases: requiresCisuConversion, requiresVersionConversion, (expected) requiresConversion values
            List<Boolean[]> testCases = Arrays.asList(
                    new Boolean[]{true,  true,  true},
                    new Boolean[]{true,  false,  true},
                    new Boolean[]{false,  true, true},
                    new Boolean[]{false,  false, false}
            );

            mockedConversionUtils.when(() -> ConversionUtils.requiresConversion(hubConfig, edxlMessage))
                    .thenCallRealMethod();

            for (int i = 0; i < testCases.size(); i++) {
                Boolean[] testCase = testCases.get(i);

                mockedConversionUtils.when(() -> ConversionUtils.requiresCisuConversion(hubConfig, edxlMessage)).thenReturn(testCase[0]);
                mockedConversionUtils.when(() -> ConversionUtils.requiresVersionConversion(hubConfig, edxlMessage)).thenReturn(testCase[1]);

                String failMessage = String.format(
                        "Test case %d failed: requiresCisuConversion=%b, requiresVersionConversion=%b, expected requiresConversion=%b",
                        i, testCase[0], testCase[1], testCase[2]
                );
                assertEquals(testCase[2], ConversionUtils.requiresConversion(hubConfig, edxlMessage), failMessage);
            }
        }
    }

    @Test
    void testRequiresVersionConversion(){
        try (MockedStatic<MessageUtils> mockedMessageUtils = mockStatic(MessageUtils.class)) {
            when(hubConfig.getVhost()).thenReturn("15-15_v2.0");
            mockedMessageUtils.when(() -> MessageUtils.getRecipientID(edxlMessage)).thenReturn("fr.health.samuV1");
            assertTrue(ConversionUtils.requiresVersionConversion(hubConfig, edxlMessage));

            when(hubConfig.getVhost()).thenReturn("15-15_v1.5");
            mockedMessageUtils.when(() -> MessageUtils.getRecipientID(edxlMessage)).thenReturn("fr.health.samuV1");
            assertFalse(ConversionUtils.requiresVersionConversion(hubConfig, edxlMessage));

            when(hubConfig.getVhost()).thenReturn("15-15_v2.0");
            mockedMessageUtils.when(() -> MessageUtils.getRecipientID(edxlMessage)).thenReturn("fr.health.samuV2");
            assertFalse(ConversionUtils.requiresVersionConversion(hubConfig, edxlMessage));

            when(hubConfig.getVhost()).thenReturn("15-15_v1.5");
            mockedMessageUtils.when(() -> MessageUtils.getRecipientID(edxlMessage)).thenReturn("fr.health.samuV2");
            assertTrue(ConversionUtils.requiresVersionConversion(hubConfig, edxlMessage));

            when(hubConfig.getVhost()).thenReturn("15-15_v2.0");
            mockedMessageUtils.when(() -> MessageUtils.getRecipientID(edxlMessage)).thenReturn("fr.health.samuA");
            assertFalse(ConversionUtils.requiresVersionConversion(hubConfig, edxlMessage));

            when(hubConfig.getVhost()).thenReturn("15-15_v1.5");
            mockedMessageUtils.when(() -> MessageUtils.getRecipientID(edxlMessage)).thenReturn("fr.health.samuA");
            assertFalse(ConversionUtils.requiresVersionConversion(hubConfig, edxlMessage));

            when(hubConfig.getVhost()).thenReturn("15-15_v2.1");
            mockedMessageUtils.when(() -> MessageUtils.getRecipientID(edxlMessage)).thenReturn("fr.health.samuA");
            assertFalse(ConversionUtils.requiresVersionConversion(hubConfig, edxlMessage));

            when(hubConfig.getVhost()).thenReturn("15-15_v1.5");
            mockedMessageUtils.when(() -> MessageUtils.getRecipientID(edxlMessage)).thenReturn("fr.health.samuNull");
            assertFalse(ConversionUtils.requiresVersionConversion(hubConfig, edxlMessage));

            when(hubConfig.getVhost()).thenReturn("15-15_v1.5");
            mockedMessageUtils.when(() -> MessageUtils.getRecipientID(edxlMessage)).thenReturn("fr.health.samuEmpty");
            assertFalse(ConversionUtils.requiresVersionConversion(hubConfig, edxlMessage));
        }
    }

    @Test
    void testGetSourceVersion(){
        when(hubConfig.getVhost()).thenReturn("15-15_v1.5");
        assertEquals("15-15_v1.5", ConversionUtils.getSourceVHost(hubConfig));

        when(hubConfig.getVhost()).thenReturn("15-15_v2.0");
        assertEquals("15-15_v2.0", ConversionUtils.getSourceVHost(hubConfig));

        when(hubConfig.getVhost()).thenReturn("15-15_v2.1");
        assertEquals("15-15_v2.1", ConversionUtils.getSourceVHost(hubConfig));

        when(hubConfig.getVhost()).thenReturn("15-nexsis_v1.9");
        assertEquals("15-nexsis_v1.9", ConversionUtils.getSourceVHost(hubConfig));
    }

    @Test
    void testGetTargetVersion(){
        try (MockedStatic<MessageUtils> mockedMessageUtils = mockStatic(MessageUtils.class)) {
            mockedMessageUtils.when(() -> MessageUtils.getRecipientID(edxlMessage)).thenReturn("fr.health.samuA");
            assertArrayEquals(new String[]{"15-15_v1.5", "15-15_v2.0","15-15_v2.1"}, ConversionUtils.getTargetVHosts(hubConfig, edxlMessage));

            mockedMessageUtils.when(() -> MessageUtils.getRecipientID(edxlMessage)).thenReturn("fr.health.samuV1");
            assertArrayEquals(new String[]{"15-15_v1.5"}, ConversionUtils.getTargetVHosts(hubConfig, edxlMessage));

            mockedMessageUtils.when(() -> MessageUtils.getRecipientID(edxlMessage)).thenReturn("fr.health.samuNull");
            assertNull(ConversionUtils.getTargetVHosts(hubConfig, edxlMessage));

            mockedMessageUtils.when(() -> MessageUtils.getRecipientID(edxlMessage)).thenReturn("fr.health.samuEmpty");
            assertArrayEquals(new String[]{},ConversionUtils.getTargetVHosts(hubConfig, edxlMessage));
        }
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
                new Boolean[]{true,  true,  false, true},
                new Boolean[]{true,  true,  true,  false},
                new Boolean[]{true,  false, false, false},
                new Boolean[]{true,  false, true,  false},
                new Boolean[]{false, true,  false, false},
                new Boolean[]{false, true,  true,  false},
                new Boolean[]{false, false, false, false},
                new Boolean[]{false, false, true,  false}
            );

            when(edxlMessage.getDescriptor().getExplicitAddress().getExplicitAddressValue()).thenReturn("fr.fire.something");
            when(hubConfig.getVhost()).thenReturn("15-15_v2.1");

            // Call the real method for requiresCisuConversion
            mockedConversionUtils.when(() -> ConversionUtils.requiresCisuConversion(hubConfig, edxlMessage))
                    .thenCallRealMethod();

            for (int i = 0; i < testCases.size(); i++) {
                Boolean[] testCase = testCases.get(i);

                // Mock the helper methods
                mockedConversionUtils.when(() -> ConversionUtils.isCisuExchange(edxlMessage)).thenReturn(testCase[0]);
                mockedConversionUtils.when(() -> ConversionUtils.isConvertedModel(edxlMessage)).thenReturn(testCase[1]);
                mockedConversionUtils.when(() -> ConversionUtils.isTargetPerimeter(hubConfig.getVhost(), MessageUtils.getRecipientID(edxlMessage))).thenReturn(false);
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

    @Test
    void testBuildExchange(){
        assertEquals("transfer_15-15_v1.5_to_15-15_v2.0", ConversionUtils.buildExchangeDestination( "15-15_v1.5", "15-15_v2.0"));
        assertEquals("transfer_toto_to_titi", ConversionUtils.buildExchangeDestination("toto", "titi"));
    }

    @Test
    void testIsTransferredToOtherVhost(){
        try (MockedStatic<ConversionUtils> mockedConversionUtils = mockStatic(ConversionUtils.class)) {
            mockedConversionUtils.when(() -> ConversionUtils.isTransferredToOtherVhost(hubConfig, edxlMessage))
                    .thenCallRealMethod();
            mockedConversionUtils.when(() -> ConversionUtils.requiresVersionConversion(hubConfig, edxlMessage))
                    .thenReturn(true);

            assertTrue(ConversionUtils.isTransferredToOtherVhost(hubConfig, edxlMessage));

            mockedConversionUtils.when(() -> ConversionUtils.requiresVersionConversion(hubConfig, edxlMessage))
                    .thenReturn(false);

            assertFalse(ConversionUtils.isTransferredToOtherVhost(hubConfig, edxlMessage));
        }
    }

    @Test
    public void isHubexVersionTest() {
        String latestSamuVHost = "15-15_v2.1";
        String previousSamuVHost = "15-15_v2.0";
        String previousHubexVHost = "15-nexsis_v1.8";
        String latestHubexVHost = "15-nexsis_v1.9";

        assertTrue(isTranscodingVersion(latestSamuVHost));
        assertFalse(isTranscodingVersion(previousSamuVHost));
        assertFalse(isTranscodingVersion(previousHubexVHost));
        assertTrue(isTranscodingVersion(latestHubexVHost));
    }

    @Test
    public void isTargetPerimeterTest() {
        assertTrue(isTargetPerimeter("15-15_v1.5", "fr.health.something"));
        assertFalse(isTargetPerimeter("15-15_v1.5", "fr.fire.something-else"));

        assertTrue(isTargetPerimeter("15-nexsis_v1.9", "fr.fire.something-else"));
        assertFalse(isTargetPerimeter("15-smur_v1.7", "fr.health.something"));
    }
}
