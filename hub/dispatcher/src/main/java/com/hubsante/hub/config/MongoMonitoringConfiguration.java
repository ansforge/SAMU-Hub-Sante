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

import org.springframework.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.mongodb.autoconfigure.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MongoMonitoringConfiguration {

    /**
     * Custom health status indicating that MongoDB is degraded but the application is still running.
     * This status should be included in management.endpoint.health.status.order.
     */
    public static final Status DEGRADED = new Status("DEGRADED");

    @Bean
    public MongoClientSettingsBuilderCustomizer mongoClusterMonitorCustomizer(
            MongoClusterMonitor monitor) {
        return builder ->
                builder.applyToClusterSettings(cluster -> cluster.addClusterListener(monitor));
    }

    /**
     * Replaces Spring Boot's default "mongo" health indicator, which pings the database on every
     * call: the state is read from the driver's background monitoring instead.
     * Toggled via management.health.mongodb.enabled (default: true)
     */
    @Bean
    @ConditionalOnEnabledHealthIndicator("mongodb")
    public HealthIndicator mongoHealthIndicator(MongoClusterMonitor monitor) {
        return () -> Health.status(monitor.isUp() ? Status.UP : DEGRADED).build();
    }
}
