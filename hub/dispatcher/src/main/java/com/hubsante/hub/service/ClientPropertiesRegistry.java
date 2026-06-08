package com.hubsante.hub.service;

import com.hubsante.hub.model.ClientProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ClientPropertiesRegistry {
    private Map<String, ClientProperties> clientsById = Map.of();

    public ClientPropertiesRegistry(
            @Value("${client.configuration.file}") Resource resource
    ) throws Exception {
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

        List<ClientProperties> clients = binder.bind(
                "clients",
                Bindable.listOf(ClientProperties.class)
        ).orElse(List.of());

        this.clientsById = clients.stream()
                .collect(Collectors.toMap(
                        ClientProperties::clientId,
                        Function.identity()
                ));
    }

    public ClientProperties get(String clientId) {
        return clientsById.get(clientId);
    }
}
