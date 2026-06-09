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

import static org.junit.jupiter.api.Assertions.*;

import com.hubsante.hub.HubApplication;
import com.hubsante.hub.exception.ClientConfigurationException;
import com.hubsante.hub.model.ClientProperties;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.test.context.SpringRabbitTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ContextConfiguration(classes = HubApplication.class)
@SpringRabbitTest
public class ClientPropertiesRegistryTest {

    static ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    @Autowired private ClientPropertiesRegistry clientPropertiesRegistry;

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry propertiesRegistry) {
        propertiesRegistry.add(
                "supported.messages.file",
                () ->
                        Objects.requireNonNull(
                                classLoader.getResource("config/supported.messages.csv")));
        propertiesRegistry.add(
                "client.preferences.file",
                () ->
                        Objects.requireNonNull(
                                classLoader.getResource("config/client.preferences.csv")));
        propertiesRegistry.add(
                "client.configuration.file",
                () -> Objects.requireNonNull(classLoader.getResource("config/clients.yaml")));
        propertiesRegistry.add("hubsante.default.message.ttl", () -> 5);
        propertiesRegistry.add("spring.rabbitmq.virtual-host", () -> "15-15_v2.1");
    }

    @Test
    @DisplayName("should load Client configuration")
    public void shouldLoadClientConfiguration() {
        assertNotNull(this.clientPropertiesRegistry);

        ClientProperties samuV1Properties = clientPropertiesRegistry.get("fr.health.test.samu-v1");
        ClientProperties samuV3Properties = clientPropertiesRegistry.get("fr.health.test.samu-v3");

        assertThrows(IllegalStateException.class, () -> clientPropertiesRegistry.get("unknown"));

        List<String> samuV1InhibitedMessages = samuV1Properties.inhibitedUseCases();
        assertNotNull(samuV1InhibitedMessages);
        assertEquals(List.of("ResourcesInfoCisuWrapper"), samuV1InhibitedMessages);
    }

    @Test
    void should_fail_when_loading_invalid_yaml() {

        Resource missingPerimeterNameYaml =
                new ClassPathResource("config/invalid-clients-missing-perimeter-name.yaml");

        assertThrows(
                ClientConfigurationException.class,
                () -> {
                    new ClientPropertiesRegistry(missingPerimeterNameYaml);
                });

        Resource missingPerimeterVersionsYaml =
                new ClassPathResource("config/invalid-clients-missing-perimeter-versions.yaml");

        assertThrows(
                ClientConfigurationException.class,
                () -> new ClientPropertiesRegistry(missingPerimeterVersionsYaml));

        Resource missingPerimetersYaml =
                new ClassPathResource("config/invalid-clients-missing-perimeters.yaml");

        assertThrows(
                ClientConfigurationException.class,
                () -> new ClientPropertiesRegistry(missingPerimetersYaml));
    }
}
