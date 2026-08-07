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

import io.micrometer.observation.ObservationPredicate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.observation.ServerRequestObservationContext;

@Configuration
public class ObservationFilterConfiguration {

    /**
     * Actuator endpoints (health probes, prometheus scraping) are polled continuously and would
     * otherwise flood the traces with meaningless spans.
     */
    @Bean
    public ObservationPredicate excludeActuatorObservations(
            @Value("${management.endpoints.web.base-path:/actuator}") String actuatorBasePath) {
        return (name, context) -> {
            if ("http.server.requests".equals(name)
                    && context instanceof ServerRequestObservationContext serverContext) {
                if (serverContext.getCarrier() != null) {
                    String uri = serverContext.getCarrier().getRequestURI();
                    return uri == null || !uri.startsWith(actuatorBasePath);
                }
            }
            return true;
        };
    }
}
