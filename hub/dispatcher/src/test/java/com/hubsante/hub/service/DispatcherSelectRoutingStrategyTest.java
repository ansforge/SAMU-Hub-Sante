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

import static com.hubsante.hub.testsupport.HubTestConstants.*;
import static com.hubsante.hub.testsupport.HubTestScaffolding.aHub;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hubsante.hub.testsupport.HubTestScaffolding;
import com.hubsante.model.edxl.Descriptor;
import com.hubsante.model.edxl.EdxlMessage;
import com.hubsante.model.edxl.ExplicitAddress;
import com.hubsante.model.report.ErrorWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Dispatcher#selectRoutingStrategy}, independent of the rest of {@code
 * dispatch()} (not wired in yet).
 */
@DisplayName("Dispatcher — selectRoutingStrategy")
class DispatcherSelectRoutingStrategyTest {

    private Dispatcher dispatcher;
    private HubTestScaffolding.Hub hub;

    @BeforeEach
    void setUp() {
        hub = aHub().build();
        dispatcher = hub.dispatcher();
    }

    private static EdxlMessage edxl(String senderId, String recipientId) {
        EdxlMessage edxlMessage = new EdxlMessage();
        edxlMessage.setSenderID(senderId);
        edxlMessage.setDistributionID(senderId + "_1234");
        edxlMessage.setDescriptor(
                new Descriptor("fr-FR", new ExplicitAddress("hubex", recipientId)));
        edxlMessage.setContentFrom(new ErrorWrapper());
        return edxlMessage;
    }

    @Test
    @DisplayName(
            "should select HubSanteInternalStrategy when both sender and recipient are health actors")
    void shouldSelectHubSanteInternalStrategy() {
        assertThat(dispatcher.selectRoutingStrategy(edxl(SAMU_A_ROUTING_KEY, SAMU_B_ROUTING_KEY)))
                .isSameAs(hub.hubSanteInternalStrategy());
    }

    @Test
    @DisplayName(
            "should select HubToFireStrategy when the sender is health and the recipient is fire")
    void shouldSelectHubToFireStrategy() {
        assertThat(dispatcher.selectRoutingStrategy(edxl(SAMU_A_ROUTING_KEY, SDIS_C_ROUTING_KEY)))
                .isSameAs(hub.hubToFireStrategy());
    }

    @Test
    @DisplayName(
            "should select FireToHubStrategy when the sender is fire and the recipient is health")
    void shouldSelectFireToHubStrategy() {
        assertThat(dispatcher.selectRoutingStrategy(edxl(SDIS_C_ROUTING_KEY, SAMU_A_ROUTING_KEY)))
                .isSameAs(hub.fireToHubStrategy());
    }

    @Test
    @DisplayName("should throw when the routing type cannot be determined")
    void shouldThrowWhenRoutingTypeUndecidable() {
        assertThatThrownBy(
                        () ->
                                dispatcher.selectRoutingStrategy(
                                        edxl(SDIS_C_ROUTING_KEY, FIRE_ROUTING_KEY)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot determine routing type");
    }
}
