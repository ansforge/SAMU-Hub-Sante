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

import static com.hubsante.hub.config.Constants.HEALTH_VHOST_PREFIX;
import static com.hubsante.hub.config.Constants.NEXSIS_VHOST;

import java.util.Set;

/**
 * This class centralizes the persistence eligibility rules
 */
public final class MessagePersistencePolicy {

    private MessagePersistencePolicy() {}

    /**
     * Message types to persist when circulating from 18 → 15 (vhost 15-nexsis).
     */
    private static final Set<String> NEXSIS_PERSISTED_USE_CASES =
            Set.of("ResourcesInfoCisuWrapper"); // RC-RI

    /**
     * Message types to persist when circulating from 15 → 18 (vhost 15-15_v*).
     */
    private static final Set<String> HEALTH_PERSISTED_USE_CASES =
            Set.of(
                    "ResourcesInfoWrapper", // RS-RI
                    "ResourcesStatusWrapper" // RS-SR
                    );

    /**
     * Returns true if the message with the given useCase from the given vhost should be persisted.
     */
    public static boolean shouldPersist(String vhost, String useCase) {
        if (NEXSIS_VHOST.equals(vhost)) {
            return NEXSIS_PERSISTED_USE_CASES.contains(useCase);
        }
        if (vhost != null && vhost.startsWith(HEALTH_VHOST_PREFIX)) {
            return HEALTH_PERSISTED_USE_CASES.contains(useCase);
        }
        return false;
    }
}
