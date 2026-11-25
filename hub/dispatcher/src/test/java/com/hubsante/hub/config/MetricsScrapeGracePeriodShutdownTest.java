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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.server.context.WebServerGracefulShutdownLifecycle;
import org.springframework.context.SmartLifecycle;

/**
 * Checks the grace period within a real Spring shutdown sequence, with stand-ins for the RabbitMQ
 * listeners and the web server at their own phases.
 */
class MetricsScrapeGracePeriodShutdownTest {

    private static final String DELAY_PROPERTY = "dispatcher.shutdown.metrics-scrape-delay";
    private static final String PHASE_TIMEOUT_PROPERTY =
            "spring.lifecycle.timeout-per-shutdown-phase";

    private final List<String> events = new CopyOnWriteArrayList<>();
    private final Map<String, Long> stoppedAt = new ConcurrentHashMap<>();
    private ListAppender<ILoggingEvent> logs;

    @BeforeEach
    void setUp() {
        logs = new ListAppender<>();
        logs.start();
        logger().addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        logger().detachAppender(logs);
    }

    private static Logger logger() {
        return (Logger) LoggerFactory.getLogger(MetricsScrapeGracePeriod.class);
    }

    private List<ILoggingEvent> warnings() {
        return logs.list.stream().filter(e -> e.getLevel() == Level.WARN).toList();
    }

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withInitializer(
                            context ->
                                    context.getBeanFactory()
                                            .setConversionService(
                                                    ApplicationConversionService
                                                            .getSharedInstance()))
                    .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                    .withBean(
                            "listeners",
                            SmartLifecycle.class,
                            () ->
                                    new RecordingLifecycle(
                                            events,
                                            stoppedAt,
                                            "listeners",
                                            SmartLifecycle.DEFAULT_PHASE))
                    .withBean(
                            "webServer",
                            SmartLifecycle.class,
                            () ->
                                    new RecordingLifecycle(
                                            events,
                                            stoppedAt,
                                            "web server",
                                            WebServerGracefulShutdownLifecycle
                                                    .SMART_LIFECYCLE_PHASE))
                    .withUserConfiguration(MetricsScrapeGracePeriod.class);

    @Test
    @DisplayName("should keep the web server up during the delay, after the listeners stopped")
    void shouldWaitBetweenListenersAndWebServer() {
        runner.withPropertyValues(DELAY_PROPERTY + "=500ms")
                .run(
                        context -> {
                            context.close();

                            assertThat(events).containsExactly("listeners", "web server");
                            // the wait takes place between the two, not before nor after them
                            assertThat(
                                            Duration.ofNanos(
                                                    stoppedAt.get("web server")
                                                            - stoppedAt.get("listeners")))
                                    .isGreaterThanOrEqualTo(Duration.ofMillis(500));
                        });
    }

    @Test
    @DisplayName("should not wait by default")
    void shouldNotWaitByDefault() {
        runner.run(
                context -> {
                    assertThat(context.getBean(MetricsScrapeGracePeriod.class).getDelay()).isZero();
                    long start = System.nanoTime();
                    context.close();

                    assertThat(events).containsExactly("listeners", "web server");
                    assertThat(Duration.ofNanos(System.nanoTime() - start))
                            // far below any real delay, with room for a loaded CI runner
                            .isLessThan(Duration.ofSeconds(2));
                });
    }

    @Test
    @DisplayName("should bind a delay expressed in seconds")
    void shouldBindDelayInSeconds() {
        runner.withPropertyValues(DELAY_PROPERTY + "=75s")
                .run(
                        context ->
                                assertThat(
                                                context.getBean(MetricsScrapeGracePeriod.class)
                                                        .getDelay())
                                        .isEqualTo(Duration.ofSeconds(75)));
    }

    @Test
    @DisplayName("should fail at startup with a negative delay")
    void shouldFailWithNegativeDelay() {
        runner.withPropertyValues(DELAY_PROPERTY + "=-1s")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("should warn at startup when Spring would give up waiting before the delay ends")
    void shouldWarnWhenPhaseTimeoutTooShort() {
        runner.withPropertyValues(DELAY_PROPERTY + "=75s", PHASE_TIMEOUT_PROPERTY + "=30s")
                .run(
                        context ->
                                assertThat(warnings())
                                        .singleElement()
                                        .extracting(ILoggingEvent::getFormattedMessage)
                                        .asString()
                                        .contains(PHASE_TIMEOUT_PROPERTY, "30s", "75s"));
    }

    @Test
    @DisplayName("should warn at startup with Spring's default phase timeout of 30s")
    void shouldWarnWithDefaultPhaseTimeout() {
        runner.withPropertyValues(DELAY_PROPERTY + "=75s")
                .run(context -> assertThat(warnings()).hasSize(1));
    }

    @Test
    @DisplayName("should not warn when the phase timeout exceeds the delay")
    void shouldNotWarnWhenPhaseTimeoutExceedsDelay() {
        runner.withPropertyValues(DELAY_PROPERTY + "=75s", PHASE_TIMEOUT_PROPERTY + "=80s")
                .run(context -> assertThat(warnings()).isEmpty());
    }

    private static class RecordingLifecycle implements SmartLifecycle {
        private final List<String> events;
        private final Map<String, Long> stoppedAt;
        private final String name;
        private final int phase;
        private volatile boolean running;

        RecordingLifecycle(
                List<String> events, Map<String, Long> stoppedAt, String name, int phase) {
            this.events = events;
            this.stoppedAt = stoppedAt;
            this.name = name;
            this.phase = phase;
        }

        @Override
        public void start() {
            running = true;
        }

        @Override
        public void stop() {
            stoppedAt.put(name, System.nanoTime());
            events.add(name);
            running = false;
        }

        @Override
        public boolean isRunning() {
            return running;
        }

        @Override
        public int getPhase() {
            return phase;
        }
    }
}
