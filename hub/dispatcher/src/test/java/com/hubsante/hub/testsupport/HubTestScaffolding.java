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
package com.hubsante.hub.testsupport;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.hubsante.hub.config.HubConfiguration;
import com.hubsante.hub.config.MappersConfiguration;
import com.hubsante.hub.service.ClientPropertiesRegistry;
import com.hubsante.hub.service.ConversionHandler;
import com.hubsante.hub.service.Dispatcher;
import com.hubsante.hub.service.MessageHandler;
import com.hubsante.hub.service.MessagePersistenceService;
import com.hubsante.hub.service.TopologyRegistry;
import com.hubsante.model.EdxlHandler;
import com.hubsante.model.Validator;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.tracing.Tracer;
import java.io.File;
import java.io.IOException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Builds a fully wired {@link Dispatcher} graph without a Spring context.
 *
 * <p>Every collaborator the dispatcher needs is either a plain object, a mock, or a spy — see {@link
 * Hub}. Build a fresh one per test method: that is what makes {@code Mockito.reset} unnecessary.
 */
public final class HubTestScaffolding {

    public static final String CLIENTS_YAML = "config/clients.yaml";
    public static final String SUPPORTED_MESSAGES_CSV = "config/supported.messages.csv";

    public static final String DEFAULT_VHOST = "15-15_v2.1";
    public static final String NEXSIS_VHOST = "15-nexsis_vactive";

    /** Matches the {@code dispatcher.default.ttl} of the packaged application.properties. */
    private static final String DEFAULT_TTL_SECONDS = "86400";

    private String vhost = DEFAULT_VHOST;
    private String ttlSeconds = DEFAULT_TTL_SECONDS;

    private HubTestScaffolding() {}

    public static HubTestScaffolding aHub() {
        return new HubTestScaffolding();
    }

    public HubTestScaffolding onVhost(String vhost) {
        this.vhost = vhost;
        return this;
    }

    public HubTestScaffolding withDefaultTtlSeconds(long seconds) {
        this.ttlSeconds = String.valueOf(seconds);
        return this;
    }

    public Hub build() {
        EdxlHandler edxlHandler = new EdxlHandler();
        MappersConfiguration mappers = new MappersConfiguration();
        XmlMapper xmlMapper = mappers.xmlMapper();
        ObjectMapper jsonMapper = mappers.jsonMapper();
        MeterRegistry registry = new SimpleMeterRegistry();

        ClientPropertiesRegistry clientPropertiesRegistry =
                spy(new ClientPropertiesRegistry(new ClassPathResource(CLIENTS_YAML)));
        // TopologyRegistry publishes itself through a static field read by ConversionUtils and
        // MessagePersistencePolicy: in production Spring builds it, here the scaffolding must.
        new TopologyRegistry(new ClassPathResource(CLIENTS_YAML));
        HubConfiguration hubConfig = spy(hubConfiguration(clientPropertiesRegistry));

        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        MessagePersistenceService persistenceService = mock(MessagePersistenceService.class);
        ConversionHandler conversionHandler =
                spy(new ConversionHandler(mock(WebClient.class), edxlHandler));
        Validator validator = spy(new Validator());

        MessageHandler messageHandler =
                spy(
                        new MessageHandler(
                                rabbitTemplate,
                                edxlHandler,
                                hubConfig,
                                validator,
                                registry,
                                xmlMapper,
                                jsonMapper,
                                conversionHandler));

        Dispatcher dispatcher =
                spy(
                        new Dispatcher(
                                messageHandler,
                                rabbitTemplate,
                                edxlHandler,
                                xmlMapper,
                                jsonMapper,
                                conversionHandler,
                                hubConfig,
                                persistenceService,
                                Tracer.NOOP));

        return new Hub(
                dispatcher,
                messageHandler,
                conversionHandler,
                rabbitTemplate,
                persistenceService,
                hubConfig,
                clientPropertiesRegistry,
                validator,
                edxlHandler,
                xmlMapper,
                jsonMapper,
                registry);
    }

    /**
     * {@link HubConfiguration} is {@code @Value}/{@code @PostConstruct} driven: populate the fields
     * the way Spring would, then run {@code init()} as a plain method.
     */
    private HubConfiguration hubConfiguration(ClientPropertiesRegistry clientPropertiesRegistry) {
        HubConfiguration hubConfig = new HubConfiguration();
        ReflectionTestUtils.setField(hubConfig, "supportedMessagesFile", supportedMessagesFile());
        ReflectionTestUtils.setField(hubConfig, "ttlProperty", ttlSeconds);
        ReflectionTestUtils.setField(hubConfig, "vhost", vhost);
        ReflectionTestUtils.setField(
                hubConfig, "clientPropertiesRegistry", clientPropertiesRegistry);
        try {
            hubConfig.init();
        } catch (Exception e) {
            throw new IllegalStateException("Could not initialise the test HubConfiguration", e);
        }
        return hubConfig;
    }

    private static File supportedMessagesFile() {
        try {
            return new ClassPathResource(SUPPORTED_MESSAGES_CSV).getFile();
        } catch (IOException e) {
            throw new IllegalStateException("Missing " + SUPPORTED_MESSAGES_CSV, e);
        }
    }

    /** The wired graph. Mocks and spies are exposed so tests can stub and verify them. */
    public record Hub(
            Dispatcher dispatcher,
            MessageHandler messageHandler,
            ConversionHandler conversionHandler,
            RabbitTemplate rabbitTemplate,
            MessagePersistenceService persistenceService,
            HubConfiguration hubConfig,
            ClientPropertiesRegistry clientPropertiesRegistry,
            Validator validator,
            EdxlHandler edxlHandler,
            XmlMapper xmlMapper,
            ObjectMapper jsonMapper,
            MeterRegistry registry) {

        /** Overrides the vhost the routing code sees, without rebuilding the graph. */
        public Hub onVhost(String vhost) {
            doReturn(vhost).when(hubConfig).getVhost();
            return this;
        }

        public void dispatch(Message message) {
            dispatcher.dispatch(message);
        }
    }
}
