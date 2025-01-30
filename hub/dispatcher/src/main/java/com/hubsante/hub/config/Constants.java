/**
 * Copyright © 2023-2025 Agence du Numerique en Sante (ANS)
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

public class Constants {

    public static final String DISPATCH_ERROR = "dispatch.error";
    public static final String DISPATCH_TIMED_METRIC = "overall.dispatch.time";
    public static final String DLQ_TIMED_METRIC = "dlq.overall.dispatch.time";
    public static final String REASON_TAG = "reason";
    public static final String CLIENT_ID_TAG = "sender";
    public static final String VHOST_TAG = "vhost";
    public static final String DISTRIBUTION_ID_UNAVAILABLE = "distributionID_could_not_be_extracted";
    public static final String DISPATCHED_MESSAGE = "dispatch.message";
    public static final String USE_CASE_TAG = "use_case";
    public static final String UNKNOWN = "unknown";
}
