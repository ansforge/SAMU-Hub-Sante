package com.hubsante.hub.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hubsante.hub.config.HubConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

public class HubConfigurationTest {
    private HubConfiguration hubConfig;

    @BeforeEach
    void setUp() throws Exception {
        hubConfig = new HubConfiguration();

        File tempFile = File.createTempFile("supported-messages", ".csv");
        try (FileWriter writer = new FileWriter(tempFile, StandardCharsets.UTF_8)) {
            writer.write("vhost;supported_messages\n");
            writer.write("common;ReferenceWrapper,ErrorWrapper\n");
            writer.write("host_1;type1,type2\n");
            writer.write("host_2;type1,type3\n");
        }

        ReflectionTestUtils.setField(hubConfig, "supportedMessagesFile", tempFile);

        File tempConfigFile = File.createTempFile("client.preferences", ".csv");
        try (FileWriter writer = new FileWriter(tempConfigFile, StandardCharsets.UTF_8)) {
            writer.write("client_id;useXML;directCISU;editor;lrm_test;15-15;15-nexsis;15-smur;15-gps;extraColumn\n");
            writer.write("fr.health.samuA;false;false;default-editor;false;1.5,2.0,2.1;1.9;1.7;2.0;extraValue\n");
            writer.write("fr.health.samuV2;false;false;default-editor;false;2.0;1.9;1.7;2.0;extraValue\n");
            writer.write("fr.health.samuV1;false;false;default-editor;false;1.5;1.9;1.7;2.0;extraValue\n");
        }

        ReflectionTestUtils.setField(hubConfig, "configFile", tempConfigFile);
    }

    @Test
    public void testSplitString() {
        String input = "apple,banana,orange";
        String[] expectedOutput = {"apple", "banana", "orange"};
        assertArrayEquals(expectedOutput, HubConfiguration.splitString(input));

        input = "apple";
        expectedOutput = new String[]{"apple"};
        assertArrayEquals(expectedOutput, HubConfiguration.splitString(input));

        input = "";
        Assertions.assertNull(HubConfiguration.splitString(input));

        input = null;
        Assertions.assertNull(HubConfiguration.splitString(input));

        // todo - enlever le cas des quotes vides
        input = ",apple,,banana,,orange,";
        expectedOutput = new String[]{"","apple", "", "banana", "", "orange"};
        assertArrayEquals(expectedOutput, HubConfiguration.splitString(input));
    }

    @Test
    void testGetSupportedMessages_host1() throws Exception {
        List<String> supportedMessages = hubConfig.loadSupportedMessages("host_1");

        Assertions.assertEquals(4, supportedMessages.size());
        Assertions.assertTrue(supportedMessages.contains("ReferenceWrapper"));
        Assertions.assertTrue(supportedMessages.contains("ErrorWrapper"));
        Assertions.assertTrue(supportedMessages.contains("type1"));
        Assertions.assertTrue(supportedMessages.contains("type2"));
    }

    @Test
    void testGetSupportedMessages_host2() throws Exception {
        List<String> supportedMessages = hubConfig.loadSupportedMessages("host_2");

        Assertions.assertEquals(4, supportedMessages.size());
        Assertions.assertTrue(supportedMessages.contains("ReferenceWrapper"));
        Assertions.assertTrue(supportedMessages.contains("ErrorWrapper"));
        Assertions.assertTrue(supportedMessages.contains("type1"));
        Assertions.assertTrue(supportedMessages.contains("type3"));
    }

    @Test
    void testGetSupportedMessages_unknownHost() throws Exception {
        List<String> supportedMessages = hubConfig.loadSupportedMessages("unknown");

        Assertions.assertEquals(2, supportedMessages.size());
        Assertions.assertTrue(supportedMessages.contains("ReferenceWrapper"));
        Assertions.assertTrue(supportedMessages.contains("ErrorWrapper"));
    }

    @Test
    void testLoadPerimeterVersions() throws Exception {
        Map<String, Map<String, String>> clientsPerimetersAndVersions = hubConfig.loadClientsPerimetersAndVersions();
        Map<String, Map<String, String>> expectedMap = new HashMap<>();

        Map<String, String> samuV1Map = new HashMap<>();
        samuV1Map.put("15-smur", "1.7");
        samuV1Map.put("15-15", "1.5");
        samuV1Map.put("15-nexsis", "1.9");
        samuV1Map.put("15-gps", "2.0");
        expectedMap.put("fr.health.samuV1", samuV1Map);

        Map<String, String> samuV2Map = new HashMap<>();
        samuV2Map.put("15-smur", "1.7");
        samuV2Map.put("15-15", "2.0");
        samuV2Map.put("15-nexsis", "1.9");
        samuV2Map.put("15-gps", "2.0");
        expectedMap.put("fr.health.samuV2", samuV2Map);

        Map<String, String> samuAMap = new HashMap<>();
        samuAMap.put("15-smur", "1.7");
        samuAMap.put("15-15", "1.5,2.0,2.1");
        samuAMap.put("15-nexsis", "1.9");
        samuAMap.put("15-gps", "2.0");
        expectedMap.put("fr.health.samuA", samuAMap);
        System.out.println(clientsPerimetersAndVersions);

        Assertions.assertEquals(clientsPerimetersAndVersions, expectedMap);
    }
}
