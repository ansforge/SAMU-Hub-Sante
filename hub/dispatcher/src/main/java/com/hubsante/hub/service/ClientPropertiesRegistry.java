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

import com.hubsante.hub.exception.ClientConfigurationException;
import com.hubsante.hub.model.ClientProperties;
import com.hubsante.hub.model.PerimeterDefinition;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class ClientPropertiesRegistry {
    private Map<String, ClientProperties> clientsById = Map.of();

    public ClientPropertiesRegistry(@Value("${client.configuration.file}") Resource resource)
            throws Exception {
        load(resource);
    }

    private void load(Resource resource) throws Exception {
        try {
            YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
            factory.setResources(resource);

            Properties props = factory.getObject();
            if (props == null) {
                throw new IllegalStateException("clients.yaml is empty");
            }

            var env = new StandardEnvironment();
            var ps = new PropertiesPropertySource("clients", props);
            env.getPropertySources().addFirst(ps);

            Binder binder = Binder.get(env);

            List<ClientProperties> clients =
                    binder.bind("clients", Bindable.listOf(ClientProperties.class))
                            .orElse(List.of());

            validateClients(clients);

            this.clientsById =
                    clients.stream()
                            .collect(
                                    Collectors.toMap(
                                            ClientProperties::clientId, Function.identity()));

        } catch (Exception e) {
            throw new ClientConfigurationException("Failed to load clients.yaml" + e.getMessage());
        }
    }

    private void validateClients(List<ClientProperties> clients) {

        for (ClientProperties client : clients) {

            if (client.clientId() == null || client.clientId().isBlank()) {
                throw new ClientConfigurationException("ClientId is missing in configuration");
            }

            if (client.perimeters() == null || client.perimeters().isEmpty()) {
                throw new ClientConfigurationException("At least one perimeter must be configured");
            }

            for (PerimeterDefinition perimeter : client.perimeters()) {
                try {
                    validatePerimeter(perimeter);
                } catch (IllegalArgumentException e) {
                    throw new ClientConfigurationException(
                            client.clientId(),
                            "Invalid perimeter configuration for client "
                                    + client.clientId()
                                    + ": "
                                    + e.getMessage());
                }
            }
        }
    }

    private void validatePerimeter(PerimeterDefinition perimeter) {

        if (perimeter.name() == null || perimeter.name().isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }

        if (perimeter.versions() == null || perimeter.versions().isEmpty()) {
            throw new IllegalArgumentException("versions must not be empty");
        }
    }

    public ClientProperties get(String clientId) {
        ClientProperties clientProperties = clientsById.get(clientId);

        if (clientProperties == null) {
            throw new IllegalStateException("client " + clientId + " is not configured");
        }

        return clientProperties;
    }
}
