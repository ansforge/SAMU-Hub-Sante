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

import java.util.Map;

public class Constants {

    public static final String DISPATCH_ERROR = "dispatch.error";
    public static final String REASON_TAG = "reason";
    public static final String CLIENT_ID_TAG = "sender";
    public static final String VHOST_TAG = "vhost";
    public static final String EDITOR_TAG = "editor";
    public static final String RECIPIENT_ID_TAG = "recipient";
    public static final String METRIC_MESSAGE_PROCESSING = "message.processing";
    public static final String OPERATION = "operation";
    public static final String DISTRIBUTION_ID_UNAVAILABLE =
            "distributionId_could_not_be_extracted";
    public static final String DISPATCHED_MESSAGE = "dispatch.message";
    public static final String USE_CASE_TAG = "use_case";
    public static final String UNKNOWN = "unknown";
    public static final String FR_HEALTH_PREFIX = "fr.health";
    public static final String FR_FIRE_PREFIX = "fr.fire";
    public static final String NEXSIS_VHOST = "15-nexsis_vactive";
    public static final String HEALTH_VHOST_PREFIX = "15-15_v";
    public static final Map<String, String> CONVERSION_VHOST_MODEL =
            Map.of(
                    "15-15_v1.5", "v1",
                    "15-15_v2.0", "v2",
                    "15-15_v2.1", "v3",
                    "15-nexsis_v1.9", "v3",
                    "15-nexsis_vactive", "vactive");

    public enum Perimeter {
        HEALTH("15-15"),
        CISU("15-nexsis"),
        GPS("15-gps"),
        SMUR("15-smur");

        private final String name;

        Perimeter(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
