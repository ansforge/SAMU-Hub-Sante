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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mongodb.connection.ClusterDescription;
import com.mongodb.event.ClusterDescriptionChangedEvent;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MongoClusterMonitorTest {

    private SimpleMeterRegistry meterRegistry;
    private MongoClusterMonitor monitor;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        monitor = new MongoClusterMonitor(meterRegistry);
    }

    private static ClusterDescriptionChangedEvent event(boolean hasWritableServer) {
        ClusterDescription description = mock(ClusterDescription.class);
        when(description.hasWritableServer()).thenReturn(hasWritableServer);
        when(description.getShortDescription()).thenReturn("{type=REPLICA_SET}");
        ClusterDescriptionChangedEvent event = mock(ClusterDescriptionChangedEvent.class);
        when(event.getNewDescription()).thenReturn(description);
        return event;
    }

    private double gaugeValue() {
        return meterRegistry.get("dispatcher_mongodb_up").gauge().value();
    }

    @Test
    @DisplayName("should be down until a writable server is reported")
    void shouldBeDownInitially() {
        assertThat(monitor.isUp()).isFalse();
        assertThat(gaugeValue()).isZero();
    }

    @Test
    @DisplayName("should be up when the cluster has a writable server")
    void shouldBeUpWhenWritableServer() {
        monitor.clusterDescriptionChanged(event(true));

        assertThat(monitor.isUp()).isTrue();
        assertThat(gaugeValue()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("should go down when the cluster loses its writable server")
    void shouldGoDownWhenWritableServerLost() {
        monitor.clusterDescriptionChanged(event(true));
        monitor.clusterDescriptionChanged(event(false));

        assertThat(monitor.isUp()).isFalse();
        assertThat(gaugeValue()).isZero();
    }

    @Test
    @DisplayName("should recover when a writable server comes back")
    void shouldRecover() {
        monitor.clusterDescriptionChanged(event(true));
        monitor.clusterDescriptionChanged(event(false));
        monitor.clusterDescriptionChanged(event(true));

        assertThat(monitor.isUp()).isTrue();
        assertThat(gaugeValue()).isEqualTo(1.0);
    }
}
