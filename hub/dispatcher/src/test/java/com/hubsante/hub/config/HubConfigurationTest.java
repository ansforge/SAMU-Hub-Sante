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
package com.hubsante.hub.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

    @ParameterizedTest(name = "vhost {0} supports {1}")
    @CsvSource({
        "host_1, 'ReferenceWrapper,ErrorWrapper,type1,type2'",
        "host_2, 'ReferenceWrapper,ErrorWrapper,type1,type3'",
        "unknown, 'ReferenceWrapper,ErrorWrapper'",
    })
    @DisplayName("should load the common messages plus the ones specific to the vhost")
    void shouldLoadSupportedMessages(String vhost, String expected) throws Exception {
        List<String> supportedMessages = hubConfig.loadSupportedMessages(vhost);

        assertThat(supportedMessages)
                .as("messages supported on vhost %s", vhost)
                .containsExactlyInAnyOrder(expected.split(","));
    }
}
