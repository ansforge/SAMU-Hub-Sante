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

import static com.hubsante.hub.config.AmqpConfiguration.DISTRIBUTION_EXCHANGE;
import static com.hubsante.hub.testsupport.HubTestScaffolding.DEFAULT_VHOST;
import static com.hubsante.hub.testsupport.HubTestScaffolding.NEXSIS_VHOST;
import static com.hubsante.hub.testsupport.HubTestScaffolding.aHub;
import static com.hubsante.hub.testsupport.MessageTestUtils.createMessage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.hubsante.hub.service.TopologyRegistry;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.util.MockUtil;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

/**
 * Guards the Spring-free wiring in {@link HubTestScaffolding}: if the production constructors or the
 * {@code @Value} field names drift, this fails here instead of in every test that uses the scaffolding.
 */
class HubTestScaffoldingTest {

    private static final String JSON = MessageProperties.CONTENT_TYPE_JSON;
    private static final String XML = MessageProperties.CONTENT_TYPE_XML;
    private static final String SAMU_B_MESSAGE_QUEUE = "fr.health.samuB.message";

    /**
     * Every collaborator a test may stub or verify must be a mock or a spy, and must be the very
     * instance wired into the graph. A plain instance here sends tests back to rebuilding the graph
     * by hand, and a spy that is not the wired one fails silently — it simply never records a call.
     */
    @Test
    @DisplayName("should expose stubbable collaborators, wired into the graph")
    void shouldExposeStubbableCollaborators() {
        HubTestScaffolding.Hub hub = aHub().build();

        assertThat(MockUtil.isMock(hub.dispatcher())).as("dispatcher is a spy").isTrue();
        assertThat(MockUtil.isMock(hub.messageHandler())).as("messageHandler is a spy").isTrue();
        assertThat(MockUtil.isMock(hub.conversionHandler()))
                .as("conversionHandler is a spy")
                .isTrue();
        assertThat(MockUtil.isMock(hub.hubConfig())).as("hubConfig is a spy").isTrue();
        assertThat(MockUtil.isMock(hub.clientPropertiesRegistry()))
                .as("clientPropertiesRegistry is a spy")
                .isTrue();
        assertThat(MockUtil.isMock(hub.validator())).as("validator is a spy").isTrue();
        assertThat(MockUtil.isMock(hub.rabbitTemplate())).as("rabbitTemplate is a mock").isTrue();
        assertThat(MockUtil.isMock(hub.persistenceService()))
                .as("persistenceService is a mock")
                .isTrue();

        assertThat(hub.hubConfig().getClientPropertiesRegistry())
                .as("hubConfig must hand out the same registry the tests stub")
                .isSameAs(hub.clientPropertiesRegistry());
    }

    @Test
    @DisplayName("should wire every collaborator of the dispatcher graph")
    void shouldWireTheWholeGraph() {
        HubTestScaffolding.Hub hub = aHub().build();

        assertThat(hub.dispatcher()).isNotNull();
        assertThat(hub.messageHandler()).isNotNull();
        assertThat(hub.conversionHandler()).isNotNull();
        assertThat(hub.registry()).isNotNull();
    }

    @Test
    @DisplayName("should run the HubConfiguration @PostConstruct so supported messages are loaded")
    void shouldInitialiseHubConfiguration() {
        HubTestScaffolding.Hub hub = aHub().build();

        assertThat(hub.hubConfig().getVhost()).isEqualTo(DEFAULT_VHOST);
        assertThat(hub.hubConfig().getSupportedMessages())
                .as("supported messages loaded from %s", HubTestScaffolding.SUPPORTED_MESSAGES_CSV)
                .isNotEmpty();
    }

    @Test
    @DisplayName("should register the TopologyRegistry singleton the static utilities read")
    void shouldRegisterTopologyRegistry() {
        aHub().build();

        assertThat(TopologyRegistry.getInstance()).isNotNull();
        assertThat(TopologyRegistry.getInstance().getMajorModelVersion(DEFAULT_VHOST)).isNotNull();
    }

    @Test
    @DisplayName("should load the client configuration from the test clients.yaml")
    void shouldLoadClientProperties() {
        HubTestScaffolding.Hub hub = aHub().build();

        assertThat(hub.clientPropertiesRegistry().get("fr.health.samuB")).isNotNull();
        assertThat(hub.clientPropertiesRegistry().get("unknown")).isNull();
    }

    @Test
    @DisplayName("should build on the requested vhost and let a test override it afterwards")
    void shouldHonourVhostOverrides() {
        assertThat(aHub().onVhost(NEXSIS_VHOST).build().hubConfig().getVhost())
                .isEqualTo(NEXSIS_VHOST);

        HubTestScaffolding.Hub hub = aHub().build();
        hub.onVhost(NEXSIS_VHOST);
        assertThat(hub.hubConfig().getVhost()).isEqualTo(NEXSIS_VHOST);
    }

    @Test
    @DisplayName("should dispatch a message end to end through the Spring-free graph")
    void shouldDispatchThroughTheGraph() throws IOException {
        HubTestScaffolding.Hub hub = aHub().build();

        hub.dispatch(createMessage("EDXL-DE", JSON));

        ArgumentCaptor<Message> sent = ArgumentCaptor.forClass(Message.class);
        verify(hub.rabbitTemplate(), times(1))
                .send(eq(DISTRIBUTION_EXCHANGE), eq(SAMU_B_MESSAGE_QUEUE), sent.capture());
        assertThat(sent.getValue().getMessageProperties().getContentType())
                .as("samuB prefers XML in the test clients.yaml")
                .isEqualTo(XML);
    }

    @Test
    @DisplayName("should isolate mocks between two graphs built in the same test")
    void shouldIsolateMocksBetweenFixtures() throws IOException {
        HubTestScaffolding.Hub first = aHub().build();
        first.dispatch(createMessage("EDXL-DE", JSON));

        HubTestScaffolding.Hub second = aHub().build();

        verify(second.rabbitTemplate(), times(0))
                .send(eq(DISTRIBUTION_EXCHANGE), eq(SAMU_B_MESSAGE_QUEUE), any());
    }
}
