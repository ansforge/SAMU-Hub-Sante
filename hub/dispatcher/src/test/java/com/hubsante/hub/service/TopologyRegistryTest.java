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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hubsante.hub.exception.ClientConfigurationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

public class TopologyRegistryTest {

    @Test
    @DisplayName("should load topology configuration, including dotted vhost keys")
    void shouldLoadTopologyConfiguration() {
        Resource resource = new ClassPathResource("config/clients.yaml");
        TopologyRegistry topologyRegistry = new TopologyRegistry(resource);

        assertEquals("v1", topologyRegistry.getMajorModelVersion("15-15_v1.5"));
        assertEquals("v3", topologyRegistry.getMajorModelVersion("15-nexsis_v1.9"));
        assertNull(topologyRegistry.getMajorModelVersion("unknown-vhost"));

        assertEquals("15-nexsis_v1.9", topologyRegistry.getVhostTarget("fire"));
        assertNull(topologyRegistry.getVhostTarget("unknown-partner"));
    }

    @Test
    @DisplayName("should fail to load when the topology block is missing")
    void shouldThrowWhenTopologyBlockMissing() {
        Resource resource = new ClassPathResource("config/invalid-clients-no-perimeters.yaml");

        assertThrows(ClientConfigurationException.class, () -> new TopologyRegistry(resource));
    }

    @Test
    @DisplayName("should fail to load when the 'fire' hubex partner entry is missing")
    void shouldThrowWhenFirePartnerMissing() {
        Resource resource =
                new ClassPathResource("config/invalid-clients-missing-fire-partner.yaml");

        assertThrows(ClientConfigurationException.class, () -> new TopologyRegistry(resource));
    }
}
