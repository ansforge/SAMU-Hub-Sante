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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Status;
import org.springframework.context.ApplicationEventPublisher;

class MongoMonitoringConfigurationTest {

    private final MongoClusterMonitor monitor =
            new MongoClusterMonitor(mock(ApplicationEventPublisher.class));

    @Test
    @DisplayName("health indicator should reflect the monitor state without querying MongoDB")
    void healthIndicatorFollowsMonitor() {
        var indicator = new MongoMonitoringConfiguration().mongoHealthIndicator(monitor);

        assertThat(indicator.health().getStatus()).isEqualTo(Status.DOWN);

        ClusterDescription description = mock(ClusterDescription.class);
        when(description.hasWritableServer()).thenReturn(true);
        ClusterDescriptionChangedEvent event = mock(ClusterDescriptionChangedEvent.class);
        when(event.getNewDescription()).thenReturn(description);
        monitor.clusterDescriptionChanged(event);

        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
    }
}
