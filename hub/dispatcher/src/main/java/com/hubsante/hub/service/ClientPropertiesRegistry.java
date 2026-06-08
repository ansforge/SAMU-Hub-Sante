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

import com.hubsante.hub.model.ClientProperties;
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
                binder.bind("clients", Bindable.listOf(ClientProperties.class)).orElse(List.of());

        this.clientsById =
                clients.stream()
                        .collect(Collectors.toMap(ClientProperties::clientId, Function.identity()));
    }

    public ClientProperties get(String clientId) {
        return clientsById.get(clientId);
    }
}
