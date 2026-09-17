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

import org.springframework.amqp.core.MessageProperties;

/** Clients, queues and content types of the test {@code config/clients.yaml}. */
public final class HubTestConstants {

    public static final String SAMU_A_ROUTING_KEY = "fr.health.samuA";
    public static final String SAMU_A_MESSAGE_QUEUE = SAMU_A_ROUTING_KEY + ".message";
    public static final String SAMU_A_INFO_QUEUE = SAMU_A_ROUTING_KEY + ".info";
    public static final String SAMU_A_DISTRIBUTION_ID =
            "fr.health.samuA_2608323d-507d-4cbf-bf74-52007f8124ea";

    public static final String SAMU_B_ROUTING_KEY = "fr.health.samuB";
    public static final String SAMU_B_MESSAGE_QUEUE = SAMU_B_ROUTING_KEY + ".message";
    public static final String SAMU_B_INFO_QUEUE = SAMU_B_ROUTING_KEY + ".info";

    public static final String SAMU_V1_ROUTING_KEY = "fr.health.samuV1";
    public static final String SAMU_V3_ROUTING_KEY = "fr.health.samuV3";
    public static final String SAMU_V3_MESSAGE_QUEUE = SAMU_V3_ROUTING_KEY + ".message";
    public static final String SAMU_V3_DIRECT_CISU_ROUTING_KEY = "fr.health.samuV3-nexsis";
    public static final String SAMU_V3_DIRECT_CISU_MESSAGE_QUEUE =
            SAMU_V3_DIRECT_CISU_ROUTING_KEY + ".message";

    public static final String SDIS_C_ROUTING_KEY = "fr.fire.sdisC";
    public static final String SDIS_C_MESSAGE_QUEUE = SDIS_C_ROUTING_KEY + ".message";
    public static final String FIRE_ROUTING_KEY = "fr.fire.sga";

    public static final String INCONSISTENT_ROUTING_KEY = "fr.health.no-samu";
    public static final String TEST_EDITOR = "default-editor";

    public static final String NEXSIS_VHOST = HubTestScaffolding.NEXSIS_VHOST;

    public static final String JSON = MessageProperties.CONTENT_TYPE_JSON;
    public static final String XML = MessageProperties.CONTENT_TYPE_XML;

    private HubTestConstants() {}
}
