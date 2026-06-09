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
import java.util.*;
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
                throw new ClientConfigurationException("clients configuration file is empty");
            }

            var env = new StandardEnvironment();
            var ps = new PropertiesPropertySource("clients", props);
            env.getPropertySources().addFirst(ps);

            Binder binder = Binder.get(env);

            List<ClientProperties> clients =
                    binder.bind("clients", Bindable.listOf(ClientProperties.class))
                            .orElseThrow(
                                    () ->
                                            new ClientConfigurationException(
                                                    "Missing 'clients' block in clients configuration file"));

            validateClients(clients);

            this.clientsById =
                    clients.stream()
                            .collect(
                                    Collectors.toMap(
                                            ClientProperties::clientId, Function.identity()));

        } catch (Exception e) {
            throw new ClientConfigurationException(e.getMessage());
        }
    }

    private void validateClients(List<ClientProperties> clients) {
        Map<String, List<String>> errorsByClient = new LinkedHashMap<>();

        for (ClientProperties client : clients) {
            List<String> errors = new ArrayList<>();

            if (client.clientId() == null || client.clientId().isBlank()) {
                errors.add("ClientId is missing in configuration");
            }

            if (client.perimeters() == null || client.perimeters().isEmpty()) {
                errors.add("At least one perimeter must be configured");
            }

            if (client.perimeters() != null) {
                for (PerimeterDefinition perimeter : client.perimeters()) {
                    try {
                        validatePerimeter(perimeter);
                    } catch (IllegalArgumentException e) {
                        errors.add(e.getMessage());
                    }
                }
            }

            if (!errors.isEmpty()) {
                errorsByClient.put(client.clientId(), errors);
            }
        }

        if (!errorsByClient.isEmpty()) {
            throw buildException(errorsByClient);
        }
    }

    private void validatePerimeter(PerimeterDefinition perimeter) {
        if (perimeter.name() == null || perimeter.name().isBlank()) {
            throw new IllegalArgumentException("Perimeter name must not be blank");
        }

        if (perimeter.versions() == null || perimeter.versions().isEmpty()) {
            throw new IllegalArgumentException("Perimeter versions must not be empty");
        }
    }

    private ClientConfigurationException buildException(Map<String, List<String>> errorsByClient) {

        StringBuilder sb = new StringBuilder("Invalid clients configuration:\n");

        errorsByClient.forEach(
                (clientId, errors) -> {
                    sb.append("\nClient: ").append(clientId).append("\n");
                    for (String error : errors) {
                        sb.append("  - ").append(error).append("\n");
                    }
                });

        return new ClientConfigurationException(sb.toString());
    }

    public ClientProperties get(String clientId) {
        ClientProperties clientProperties = clientsById.get(clientId);

        if (clientProperties == null) {
            throw new ClientConfigurationException("client " + clientId + " is not configured");
        }

        return clientProperties;
    }
}
