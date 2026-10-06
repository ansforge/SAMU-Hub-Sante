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
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
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

    private static final Duration INITIAL_RETRY_DELAY = Duration.ofSeconds(1);
    private static final Duration MAX_RETRY_DELAY = Duration.ofMinutes(5);

    private final MongoTemplate mongoTemplate;

    @Value("${persistence.arrivedAt.expiresDurationDays}")
    private int expiresDurationDays;

    private final ScheduledExecutorService retryScheduler =
            Executors.newSingleThreadScheduledExecutor(
                    runnable -> {
                        Thread thread = new Thread(runnable, "mongo-index-creation-retry");
                        thread.setDaemon(true);
                        return thread;
                    });

    public MongoConfiguration(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void createIndexes() {
        attemptCreateIndexes(INITIAL_RETRY_DELAY);
    }

    private void attemptCreateIndexes(Duration nextRetryDelay) {
        try {
            doCreateIndexes();
            log.info("MongoDB indexes successfully created");
        } catch (Exception e) {
            log.error(
                    "Failed to create MongoDB indexes, the database appears to be unreachable. "
                            + "The application will keep running without them and retry in {}s.",
                    nextRetryDelay.toSeconds(),
                    e);
            retryScheduler.schedule(
                    () -> attemptCreateIndexes(nextDelay(nextRetryDelay)),
                    nextRetryDelay.toMillis(),
                    TimeUnit.MILLISECONDS);
        }
    }

    private static Duration nextDelay(Duration currentDelay) {
        Duration doubled = currentDelay.multipliedBy(2);
        return doubled.compareTo(MAX_RETRY_DELAY) > 0 ? MAX_RETRY_DELAY : doubled;
    }

    @PreDestroy
    public void shutdownRetryScheduler() {
        retryScheduler.shutdownNow();
    }

    private void doCreateIndexes() {
        var indexOps = mongoTemplate.indexOps(PersistedMessage.COLLECTION_NAME);
        indexOps.createIndex(
                new Index()
                        .named("idx_arrivedAt_ttl")
                        .on("arrivedAt", Sort.Direction.ASC)
                        .expire(Duration.ofDays(expiresDurationDays)));
        indexOps.createIndex(new Index().named("idx_message_type").on("type", Sort.Direction.ASC));
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
                                        Criteria.where("type").is("ResourcesInfoCisuWrapper"))));
    }
}
