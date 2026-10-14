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
import java.util.concurrent.ScheduledFuture;
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

    private final MongoTemplate mongoTemplate;
    private final MongoClusterMonitor mongoMonitor;

    @Value("${persistence.arrivedAt.expiresDurationDays}")
    private int expiresDurationDays;

    @Value("${mongo.index-creation-retry.initial-delay-ms:1000}")
    private long initialRetryDelayMs;

    @Value("${mongo.index-creation-retry.max-delay-ms:300000}")
    private long maxRetryDelayMs;

    private final ScheduledExecutorService retryScheduler =
            Executors.newSingleThreadScheduledExecutor(
                    runnable -> {
                        Thread thread = new Thread(runnable, "mongo-index-creation-retry");
                        thread.setDaemon(true);
                        return thread;
                    });

    // Lock object to guard index creation and the pending retry, as attempts come from several threads
    private final Object lock = new Object();
    private ScheduledFuture<?> pendingRetry;

    public MongoConfiguration(MongoTemplate mongoTemplate, MongoClusterMonitor mongoMonitor) {
        this.mongoTemplate = mongoTemplate;
        this.mongoMonitor = mongoMonitor;
    }

    /**
     * Creates MongoDB indexes once the application is ready. If MongoDB is not reachable yet,
     * the creation will be triggered once the connection is restored.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void createIndexes() {
        if (mongoMonitor.isUp()) {
            startAttempts();
        } else {
            log.warn("MongoDB is not reachable yet, indexes will be created once it is");
        }
    }

    /**
     * Handles the event when the MongoDB connection is restored.
     * Triggers the creation of indexes.
     */
    @EventListener(MongoConnectionRestoredEvent.class)
    public void onMongoConnectionRestored() {
        log.info("MongoDB connection restored, creating indexes");
        retryScheduler.execute(this::startAttempts);
    }

    // Starts the attempts to create indexes, cancelling any pending retry if necessary.
    private void startAttempts() {
        synchronized (lock) {
            if (pendingRetry != null) {
                pendingRetry.cancel(false);
                pendingRetry = null;
            }
            attemptCreateIndexes(Duration.ofMillis(initialRetryDelayMs));
        }
    }


    /**
     * Attempts to create MongoDB indexes. If the creation fails while MongoDB is up,
     * it will schedule a retry with exponential backoff. If MongoDB is down, it will
     * wait for the connection to be restored before retrying.
     */
    private void attemptCreateIndexes(Duration nextRetryDelay) {
        synchronized (lock) {
            try {
                doCreateIndexes();
                log.info("MongoDB indexes successfully created");
            } catch (Exception e) {
                if (!mongoMonitor.isUp()) {
                    log.error(
                            "Failed to create MongoDB indexes, the database is down. "
                                    + "They will be created once the connection is restored.",
                            e);
                    return;
                }
                log.error(
                        "Failed to create MongoDB indexes while the database is up, "
                                + "retrying in {}s.",
                        nextRetryDelay.toSeconds(),
                        e);
                pendingRetry =
                        retryScheduler.schedule(
                                () -> attemptCreateIndexes(nextDelay(nextRetryDelay)),
                                nextRetryDelay.toMillis(),
                                TimeUnit.MILLISECONDS);
            }
        }
    }

    private Duration nextDelay(Duration currentDelay) {
        Duration doubled = currentDelay.multipliedBy(2);
        Duration maxRetryDelay = Duration.ofMillis(maxRetryDelayMs);
        return doubled.compareTo(maxRetryDelay) > 0 ? maxRetryDelay : doubled;
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
