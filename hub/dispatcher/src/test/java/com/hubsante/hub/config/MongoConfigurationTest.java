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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.test.util.ReflectionTestUtils;

class MongoConfigurationTest {

    // number of indexes created by MongoConfiguration
    private static final int INDEX_COUNT = 7;

    private MongoTemplate mongoTemplate;
    private MongoClusterMonitor mongoMonitor;
    private MongoConfiguration mongoConfiguration;

    @BeforeEach
    void setUp() {
        mongoTemplate = mock(MongoTemplate.class);
        mongoMonitor = mock(MongoClusterMonitor.class);
        when(mongoMonitor.isUp()).thenReturn(true);
        mongoConfiguration = new MongoConfiguration(mongoTemplate, mongoMonitor);
        ReflectionTestUtils.setField(mongoConfiguration, "expiresDurationDays", 3);
        ReflectionTestUtils.setField(mongoConfiguration, "initialRetryDelayMs", 50L);
        ReflectionTestUtils.setField(mongoConfiguration, "maxRetryDelayMs", 200L);
    }

    @AfterEach
    void tearDown() {
        mongoConfiguration.shutdownRetryScheduler();
    }

    private static DataAccessResourceFailureException mongoDown() {
        return new DataAccessResourceFailureException(
                "Connection refused: no MongoDB server available");
    }

    @Test
    @DisplayName("should create indexes at startup when MongoDB is already up")
    void shouldCreateIndexesAtStartupWhenUp() {
        IndexOperations indexOperations = mock(IndexOperations.class);
        when(mongoTemplate.indexOps(anyString())).thenReturn(indexOperations);

        mongoConfiguration.createIndexes();

        verify(indexOperations, times(INDEX_COUNT)).createIndex(any());
    }

    @Test
    @DisplayName("should wait for the restored event at startup when MongoDB is not up yet")
    void shouldNotCreateIndexesAtStartupWhenDown() {
        when(mongoMonitor.isUp()).thenReturn(false);

        assertThatCode(() -> mongoConfiguration.createIndexes()).doesNotThrowAnyException();

        verify(mongoTemplate, never()).indexOps(anyString());
    }

    @Test
    @DisplayName("should create indexes when the MongoDB connection is restored")
    void shouldCreateIndexesOnConnectionRestored() {
        IndexOperations indexOperations = mock(IndexOperations.class);
        when(mongoTemplate.indexOps(anyString())).thenReturn(indexOperations);

        mongoConfiguration.onMongoConnectionRestored();

        await().atMost(Duration.ofSeconds(2))
                .untilAsserted(
                        () -> verify(indexOperations, times(INDEX_COUNT)).createIndex(any()));
    }

    @Test
    @DisplayName("should retry with backoff while MongoDB is up")
    void shouldRetryWhileMongoIsUp() {
        IndexOperations indexOperations = mock(IndexOperations.class);
        AtomicInteger indexOpsCalls = new AtomicInteger();
        when(mongoTemplate.indexOps(anyString()))
                .thenAnswer(
                        invocation -> {
                            if (indexOpsCalls.getAndIncrement() < 2) {
                                throw new DataAccessResourceFailureException("index build failed");
                            }
                            return indexOperations;
                        });

        mongoConfiguration.createIndexes();

        // first call fails immediately, then retries after 50ms, then 100ms: allow up to 2s
        await().atMost(Duration.ofSeconds(2))
                .untilAsserted(
                        () -> verify(indexOperations, times(INDEX_COUNT)).createIndex(any()));
    }

    @Test
    @DisplayName("should not retry when creation fails because MongoDB is down")
    void shouldNotRetryWhenMongoIsDown() throws InterruptedException {
        when(mongoTemplate.indexOps(anyString())).thenThrow(mongoDown());
        when(mongoMonitor.isUp()).thenReturn(true, false);

        assertThatCode(() -> mongoConfiguration.createIndexes()).doesNotThrowAnyException();

        Thread.sleep(400);
        verify(mongoTemplate, times(1)).indexOps(anyString());
    }

    @Test
    @DisplayName("should not propagate a failure when creating indexes after a reconnection")
    void shouldNotThrowWhenCreationFailsAfterReconnection() {
        when(mongoTemplate.indexOps(anyString())).thenThrow(mongoDown());

        assertThatCode(() -> mongoConfiguration.onMongoConnectionRestored())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("a new reconnection should supersede a retry pending from a previous failure")
    void shouldSupersedePendingRetryOnReconnection() {
        IndexOperations indexOperations = mock(IndexOperations.class);
        AtomicInteger indexOpsCalls = new AtomicInteger();
        when(mongoTemplate.indexOps(anyString()))
                .thenAnswer(
                        invocation -> {
                            if (indexOpsCalls.getAndIncrement() == 0) {
                                throw new DataAccessResourceFailureException("index build failed");
                            }
                            return indexOperations;
                        });
        ReflectionTestUtils.setField(mongoConfiguration, "initialRetryDelayMs", 60_000L);

        mongoConfiguration.createIndexes(); // fails, retry scheduled in 60s
        mongoConfiguration.onMongoConnectionRestored(); // succeeds right away

        await().atMost(Duration.ofSeconds(2))
                .untilAsserted(
                        () -> verify(indexOperations, times(INDEX_COUNT)).createIndex(any()));
    }
}
