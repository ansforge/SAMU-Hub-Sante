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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
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

    private MongoTemplate mongoTemplate;
    private MongoConfiguration mongoConfiguration;

    @BeforeEach
    void setUp() {
        mongoTemplate = mock(MongoTemplate.class);
        mongoConfiguration = new MongoConfiguration(mongoTemplate);
        ReflectionTestUtils.setField(mongoConfiguration, "expiresDurationDays", 3);
        ReflectionTestUtils.setField(mongoConfiguration, "initialRetryDelayMs", 50L);
        ReflectionTestUtils.setField(mongoConfiguration, "maxRetryDelayMs", 200L);
    }

    @AfterEach
    void tearDown() {
        mongoConfiguration.shutdownRetryScheduler();
    }

    @Test
    @DisplayName("should not throw when MongoDB is unreachable during index creation")
    void shouldNotThrowWhenMongoIsUnreachable() {
        when(mongoTemplate.indexOps(anyString()))
                .thenThrow(
                        new DataAccessResourceFailureException(
                                "Connection refused: no MongoDB server available"));

        assertThatCode(() -> mongoConfiguration.createIndexes()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should not throw when an individual index creation fails")
    void shouldNotThrowWhenCreateIndexFails() {
        IndexOperations indexOperations = mock(IndexOperations.class);
        when(mongoTemplate.indexOps(anyString())).thenReturn(indexOperations);
        when(indexOperations.createIndex(any()))
                .thenThrow(
                        new DataAccessResourceFailureException(
                                "Connection refused: no MongoDB server available"));

        assertThatCode(() -> mongoConfiguration.createIndexes()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should retry index creation with backoff until MongoDB becomes reachable")
    void shouldRetryUntilMongoIsReachable() {
        IndexOperations indexOperations = mock(IndexOperations.class);
        AtomicInteger indexOpsCalls = new AtomicInteger();
        when(mongoTemplate.indexOps(anyString()))
                .thenAnswer(
                        invocation -> {
                            if (indexOpsCalls.getAndIncrement() < 2) {
                                throw new DataAccessResourceFailureException(
                                        "Connection refused: no MongoDB server available");
                            }
                            return indexOperations;
                        });

        mongoConfiguration.createIndexes();

        // first call fails immediately, then retries after 50ms, then 100ms: allow up to 2s
        await().atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> assertThat(indexOpsCalls.get()).isEqualTo(3));
    }
}
