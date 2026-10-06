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

import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.mongodb.autoconfigure.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for customizing MongoDB connection timeouts.
 */
@Configuration
public class MongoConnectionConfiguration {

    @Value("${mongo.connection.server-selection-timeout-ms:5000}")
    private long serverSelectionTimeoutMs;

    @Value("${mongo.connection.connect-timeout-ms:5000}")
    private int connectTimeoutMs;

    /**
     * Shortens the MongoDB driver's default timeouts so that any operation fails fast when
     * the database is unreachable.
     */
    @Bean
    public MongoClientSettingsBuilderCustomizer mongoConnectionTimeoutCustomizer() {
        return builder ->
                builder.applyToClusterSettings(
                                cluster ->
                                        cluster.serverSelectionTimeout(
                                                serverSelectionTimeoutMs, TimeUnit.MILLISECONDS))
                        .applyToSocketSettings(
                                socket ->
                                        socket.connectTimeout(
                                                connectTimeoutMs, TimeUnit.MILLISECONDS));
    }
}
