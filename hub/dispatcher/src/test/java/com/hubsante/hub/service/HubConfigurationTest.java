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

import com.hubsante.hub.config.HubConfiguration;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
}
