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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import static com.hubsante.hub.utils.ConversionUtils.isAlreadyCisuConverted;
import static com.hubsante.hub.utils.ConversionUtils.trimVersionSuffix;
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

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        directCisuPreferences = new HashMap<>();
        directCisuPreferences.put("fr.health.samuDirectCisu", true);

        when(hubConfig.getDirectCisuPreferences()).thenReturn(directCisuPreferences);
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

    @ParameterizedTest
    @MethodSource("provideVersionConversionCases")
    void testRequiresVersionConversion(String vhost,
                                       String senderId,
                                       String recipientId,
                                       String perimeter,
                                       String[] versions,
                                       boolean expected) {
        try (MockedStatic<MessageUtils> mockedMessageUtils = mockStatic(MessageUtils.class)) {
            when(hubConfig.getVhost()).thenReturn(vhost);
            when(edxlMessage.getSenderID()).thenReturn(senderId);
            mockedMessageUtils.when(() -> MessageUtils.getRecipientID(edxlMessage))
                    .thenReturn(recipientId);
            when(hubConfig.getClientVersionsForPerimeter(recipientId, perimeter))
                    .thenReturn(versions);

            assertEquals(expected, ConversionUtils.requiresVersionConversion(hubConfig, edxlMessage));
        }
    }

    private static Stream<Arguments> provideVersionConversionCases() {
        return Stream.of(
                // Case 1: conversion 15-15 2 -> 1
                Arguments.of("15-15_v2.0", "fr.health.samuV2", "fr.health.samuV1", "15-15", new String[]{"1.5"}, true),
                // Case 2: no conversion 15-15 1 -> 1
                Arguments.of("15-15_v1.5", "fr.health.samuX", "fr.health.samuV1", "15-15", new String[]{"1.5"}, false),
                // Case 3: no conversion 15-15 2 -> 2
                Arguments.of("15-15_v2.0", "fr.health.samuX", "fr.health.samuV2", "15-15", new String[]{"2.0"}, false),
                // Case 4: conversion 15-15 1 -> 2
                Arguments.of("15-15_v1.5", "fr.health.samuV1", "fr.health.samuV2", "15-15", new String[]{"2.0"}, true),
                // Case 5: no conversion 15-15 2 -> 1,2
                Arguments.of("15-15_v2.0", "fr.health.samuVX", "fr.health.samuA", "15-15", new String[]{"1.5", "2.0", "2.1"}, false),
                // Case 6: no conversion 15-15 1 -> 1,2
                Arguments.of("15-15_v1.5", "fr.health.samuVX", "fr.health.samuA", "15-15", new String[]{"1.5", "2.0", "2.1"}, false),
                // Case 7: no conversion 15-15 3 -> 1,2,3
                Arguments.of("15-15_v2.1", "fr.health.samuVX", "fr.health.samuA", "15-15", new String[]{"1.5", "2.0", "2.1"}, false),
                // Case 8: no conversion 15-15 2 -> null
                Arguments.of("15-15_v1.5", "fr.health.samuVX", "fr.health.samuNull", "15-15", null, false),
                // Case 9: no conversion 15-15 2 -> empty
                Arguments.of("15-15_v1.5", "fr.health.samuVX", "fr.health.samuEmpty", "15-15", new String[]{}, false),
                // Case 10: conversion 15-smur 2 -> 3
                Arguments.of("15-smur_v1.6", "fr.health.samuV2", "fr.health.samuV3", "15-smur", new String[]{"1.7"}, true),
                // Case 11: conversion 15-smur 3 -> 2
                Arguments.of("15-smur_v1.7", "fr.health.samuV3", "fr.health.samuV2", "15-smur", new String[]{"1.6"}, true),
                // Case 12: no conversion 15-smur 3 -> 3
                Arguments.of("15-smur_v1.7", "fr.health.samuV3", "fr.health.samuV3bis", "15-smur", new String[]{"1.7"}, false)
        );
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
            when(hubConfig.getVhost()).thenReturn("15-15_v2.1");

            when(edxlMessage.getSenderID()).thenReturn("fr.health.samuVX");

            when(hubConfig.getClientVersionsForPerimeter("fr.health.samuA", "15-15")).thenReturn(new String[]{"1.5", "2.0", "2.1"});
            mockedMessageUtils.when(() -> MessageUtils.getRecipientID(edxlMessage)).thenReturn("fr.health.samuA");
            assertArrayEquals(new String[]{"15-15_v1.5", "15-15_v2.0","15-15_v2.1"}, ConversionUtils.getTargetVHosts(hubConfig, edxlMessage));

            when(hubConfig.getClientVersionsForPerimeter("fr.health.samuV1", "15-15")).thenReturn(new String[]{"1.5"});
            mockedMessageUtils.when(() -> MessageUtils.getRecipientID(edxlMessage)).thenReturn("fr.health.samuV1");
            assertArrayEquals(new String[]{"15-15_v1.5"}, ConversionUtils.getTargetVHosts(hubConfig, edxlMessage));

            when(hubConfig.getClientVersionsForPerimeter("fr.health.samuNull", "15-15")).thenReturn(null);
            mockedMessageUtils.when(() -> MessageUtils.getRecipientID(edxlMessage)).thenReturn("fr.health.samuNull");
            assertArrayEquals(null,ConversionUtils.getTargetVHosts(hubConfig, edxlMessage));

            when(hubConfig.getClientVersionsForPerimeter("fr.health.samuEmpty", "15-15")).thenReturn(new String[]{});
            mockedMessageUtils.when(() -> MessageUtils.getRecipientID(edxlMessage)).thenReturn("fr.health.samuEmpty");
            assertArrayEquals(new String[]{},ConversionUtils.getTargetVHosts(hubConfig, edxlMessage));

            // CISU exchange cases
            when(hubConfig.getVhost()).thenReturn("15-nexsis_v1.9");

            when(edxlMessage.getSenderID()).thenReturn("fr.fire.test");
            mockedMessageUtils.when(() -> MessageUtils.getRecipientID(edxlMessage)).thenReturn("fr.health.samuV1");
            when(hubConfig.getClientVersionsForPerimeter("fr.health.samuV1", "15-15")).thenReturn(new String[]{"1.5"});
            assertArrayEquals(new String[]{"15-15_v1.5"}, ConversionUtils.getTargetVHosts(hubConfig, edxlMessage));

            when(edxlMessage.getSenderID()).thenReturn("fr.cisu.test");
            assertArrayEquals(new String[]{"15-15_v1.5"}, ConversionUtils.getTargetVHosts(hubConfig, edxlMessage));

            when(hubConfig.getVhost()).thenReturn("15-15_v1.5");
            when(edxlMessage.getSenderID()).thenReturn("fr.health.samuV1");
            mockedMessageUtils.when(() -> MessageUtils.getRecipientID(edxlMessage)).thenReturn("fr.fire.test");
            assertArrayEquals(new String[]{"15-nexsis_v1.9"}, ConversionUtils.getTargetVHosts(hubConfig, edxlMessage));

            mockedMessageUtils.when(() -> MessageUtils.getRecipientID(edxlMessage)).thenReturn("fr.cisu.test");
            assertArrayEquals(new String[]{"15-nexsis_v1.9"}, ConversionUtils.getTargetVHosts(hubConfig, edxlMessage));

            // Direct Cisu cases
            when(hubConfig.getVhost()).thenReturn("15-nexsis_v1.9");
            when(edxlMessage.getSenderID()).thenReturn("fr.health.samuDirectCisu");
            mockedMessageUtils.when(() -> MessageUtils.getRecipientID(edxlMessage)).thenReturn("fr.fire.test");
            assertArrayEquals(new String[]{"15-nexsis_v1.9"}, ConversionUtils.getTargetVHosts(hubConfig, edxlMessage));

            when(edxlMessage.getSenderID()).thenReturn("fr.fire.test");
            when(hubConfig.getClientVersionsForPerimeter("fr.health.samuDirectCisu", "15-nexsis")).thenReturn(new String[]{"1.9"});
            mockedMessageUtils.when(() -> MessageUtils.getRecipientID(edxlMessage)).thenReturn("fr.health.samuDirectCisu");
            assertArrayEquals(new String[]{"15-nexsis_v1.9"}, ConversionUtils.getTargetVHosts(hubConfig, edxlMessage));
        }
    }

    @Test
    void testIsCisuExchange() {
        try (MockedStatic<MessageUtils> mockedMessageUtils = mockStatic(MessageUtils.class)) {
            // Health to Health (false)
            when(edxlMessage.getSenderID()).thenReturn("fr.health.samuA");
            mockedMessageUtils.when(() -> MessageUtils.getRecipientID(edxlMessage)).thenReturn("fr.health.samuB");
            assertFalse(ConversionUtils.isOneCisuHubexInvolved(edxlMessage));

            // Health to CISU (true)
            mockedMessageUtils.when(() -> MessageUtils.getRecipientID(edxlMessage)).thenReturn("fr.fire.sdisZ");
            assertTrue(ConversionUtils.isOneCisuHubexInvolved(edxlMessage));

            // CISU to Health (true)
            when(edxlMessage.getSenderID()).thenReturn("fr.fire.sdisZ");
            mockedMessageUtils.when(() -> MessageUtils.getRecipientID(edxlMessage)).thenReturn("fr.health.samuA");
            assertTrue(ConversionUtils.isOneCisuHubexInvolved(edxlMessage));
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
                mockedConversionUtils.when(() -> ConversionUtils.isOneCisuHubexInvolved(edxlMessage)).thenReturn(testCase[0]);
                mockedConversionUtils.when(() -> ConversionUtils.isConvertedModel(edxlMessage)).thenReturn(testCase[1]);
                mockedConversionUtils.when(() -> ConversionUtils.isAlreadyCisuConverted(hubConfig.getVhost(), MessageUtils.getRecipientID(edxlMessage))).thenReturn(false);
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
    public void isAlreadyCisuConvertedTest() {
        assertTrue(isAlreadyCisuConverted("15-15_v1.5", "fr.health.something"));
        assertTrue(isAlreadyCisuConverted("15-nexsis_v1.9", "fr.fire.something-else"));

        assertFalse(isAlreadyCisuConverted("15-15_v1.5", "fr.fire.something-else"));
        assertFalse(isAlreadyCisuConverted("15-nexsis_v1.9", "fr.health.something"));
        assertFalse(isAlreadyCisuConverted("15-smur_v1.7", "fr.health.something"));
    }

    @Test
    public void testTrimVersionSuffix() {
        assertEquals("15-15", trimVersionSuffix("15-15_v1.3"));
        assertEquals("15-nexsis", trimVersionSuffix("15-nexsis_v2"));
        assertEquals("backup", trimVersionSuffix("backup_v2.0.1"));
        assertEquals("no-version-here", trimVersionSuffix("no-version-here"));
        assertNull(trimVersionSuffix(null));
        assertEquals("",trimVersionSuffix(""));
    }

    @Test
    void testFormatVersionToVhosts() {
        String[] versions = {"1.5", "2.0"};
        String[] expected = {"15-15_v1.5", "15-15_v2.0"};
        assertArrayEquals(expected, ConversionUtils.formatVersionToVhosts(versions, "15-15"));

        versions = new String[]{"1.5", "2.0"};
        expected = new String[]{"15-smur_v1.5", "15-smur_v2.0"};
        assertArrayEquals(expected, ConversionUtils.formatVersionToVhosts(versions, "15-smur"));

        String[] empty = {};
        String[] expectedEmpty = {};
        assertArrayEquals(expectedEmpty, ConversionUtils.formatVersionToVhosts(empty, "15-15"));

        assertNull(ConversionUtils.formatVersionToVhosts(null, "15-15"));
    }

    @Test
    public void testIsConversionAvailable() {
        assertTrue(ConversionUtils.isConversionAvailable("15-15_v1.5"));
        assertTrue(ConversionUtils.isConversionAvailable("15-15_v2.1"));
        assertTrue(ConversionUtils.isConversionAvailable("15-15_v2.0"));
        assertTrue(ConversionUtils.isConversionAvailable("15-nexsis_v1.9"));
        assertTrue(ConversionUtils.isConversionAvailable("15-smur_v1.6"));
        assertTrue(ConversionUtils.isConversionAvailable("15-smur_v1.7"));
        assertFalse(ConversionUtils.isConversionAvailable("other_vhost"));
    }
}
