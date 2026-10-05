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

import com.hubsante.hub.model.PersistedMessage;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.mongodb.autoconfigure.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.PartialIndexFilter;
import org.springframework.data.mongodb.core.query.Criteria;

@Configuration
@EnableMongoAuditing
@Slf4j
public class MongoConfiguration {

    private final MongoTemplate mongoTemplate;

    @Value("${persistence.arrivedAt.expiresDurationDays}")
    private int expiresDurationDays;

    @Value("${mongo.connection.server-selection-timeout-ms:5000}")
    private long serverSelectionTimeoutMs;

    @Value("${mongo.connection.connect-timeout-ms:5000}")
    private int connectTimeoutMs;

    /**
     * Shortens the MongoDB driver's default timeouts (30s) so that any operation fails fast
     * when the database is unreachable, instead of blocking the caller (e.g. index creation at
     * startup, or message persistence) for a long time.
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

    public MongoConfiguration(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void createIndexes() {
        try {
            var indexOps = mongoTemplate.indexOps(PersistedMessage.COLLECTION_NAME);
            indexOps.createIndex(
                    new Index()
                            .named("idx_arrivedAt_ttl")
                            .on("arrivedAt", Sort.Direction.ASC)
                            .expire(Duration.ofDays(expiresDurationDays)));
            indexOps.createIndex(
                    new Index().named("idx_message_type").on("type", Sort.Direction.ASC));
            indexOps.createIndex(
                    new Index()
                            .named("idx_distributionID")
                            .on("payload.distributionID", Sort.Direction.ASC));
            indexOps.createIndex(
                    new Index()
                            .named("idx_resourcesInfo_caseId")
                            .on(
                                    "payload.content.jsonContent.embeddedJsonContent.message.resourcesInfo.caseId",
                                    Sort.Direction.ASC)
                            .partial(
                                    PartialIndexFilter.of(
                                            Criteria.where("type").is("ResourcesInfoWrapper"))));
            indexOps.createIndex(
                    new Index()
                            .named("idx_resourcesStatus_caseId")
                            .on(
                                    "payload.content.jsonContent.embeddedJsonContent.message.resourcesStatus.caseId",
                                    Sort.Direction.ASC)
                            .partial(
                                    PartialIndexFilter.of(
                                            Criteria.where("type").is("ResourcesStatusWrapper"))));
            indexOps.createIndex(
                    new Index()
                            .named("idx_resourcesStatus_resourceId")
                            .on(
                                    "payload.content.jsonContent.embeddedJsonContent.message.resourcesStatus.resourceId",
                                    Sort.Direction.ASC)
                            .partial(
                                    PartialIndexFilter.of(
                                            Criteria.where("type").is("ResourcesStatusWrapper"))));
            indexOps.createIndex(
                    new Index()
                            .named("idx_resourcesInfoCisu_caseId")
                            .on(
                                    "payload.content.jsonContent.embeddedJsonContent.message.resourcesInfoCisu.caseId",
                                    Sort.Direction.ASC)
                            .partial(
                                    PartialIndexFilter.of(
                                            Criteria.where("type")
                                                    .is("ResourcesInfoCisuWrapper"))));
        } catch (Exception e) {
            log.error(
                    "Failed to create MongoDB indexes, the database appears to be unreachable. "
                            + "The application will keep starting without them.",
                    e);
        }
    }
}
