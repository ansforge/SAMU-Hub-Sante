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

import static com.hubsante.hub.config.Constants.DISPATCHED_MESSAGE;
import static com.hubsante.hub.config.Constants.DISPATCH_ERROR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.boot.web.server.context.WebServerGracefulShutdownLifecycle;

class MetricsScrapeGracePeriodTest {

    private SimpleMeterRegistry meterRegistry;
    private ListAppender<ILoggingEvent> logs;

    // delayed tasks are captured instead of being scheduled, so that the tests decide when they run
    private final List<Duration> requestedDelays = new ArrayList<>();
    private final List<Runnable> delayedTasks = new ArrayList<>();

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
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

    private static final Duration PHASE_TIMEOUT = Duration.ofSeconds(80);

    private MetricsScrapeGracePeriod gracePeriod(Duration delay) {
        return gracePeriod(delay, PHASE_TIMEOUT);
    }

    private MetricsScrapeGracePeriod gracePeriod(Duration delay, Duration phaseTimeout) {
        return new MetricsScrapeGracePeriod(
                delay,
                phaseTimeout,
                meterRegistry,
                d -> {
                    requestedDelays.add(d);
                    return (Executor) delayedTasks::add;
                });
    }

    private List<String> logMessages() {
        return logs.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }

    @Test
    @DisplayName("should stop between the RabbitMQ listeners and the web server graceful shutdown")
    void shouldStopBetweenListenersAndWebServer() {
        int phase = gracePeriod(Duration.ZERO).getPhase();

        // phases stop in decreasing order
        assertThat(phase)
                .isLessThan(new RabbitListenerEndpointRegistry().getPhase())
                .isGreaterThan(WebServerGracefulShutdownLifecycle.SMART_LIFECYCLE_PHASE);
    }

    @Test
    @DisplayName("should reflect its real state")
    void shouldReflectRunningState() {
        MetricsScrapeGracePeriod gracePeriod = gracePeriod(Duration.ofSeconds(75));
        assertThat(gracePeriod.isRunning()).isFalse();

        gracePeriod.start();
        assertThat(gracePeriod.isRunning()).isTrue();

        gracePeriod.stop(() -> {});
        assertThat(gracePeriod.isRunning()).isFalse();
    }

    @Test
    @DisplayName("should stop synchronously without waiting")
    void shouldStopSynchronously() {
        MetricsScrapeGracePeriod gracePeriod = gracePeriod(Duration.ofSeconds(75));
        gracePeriod.start();

        gracePeriod.stop();

        assertThat(gracePeriod.isRunning()).isFalse();
        assertThat(delayedTasks).isEmpty();
    }

    @Test
    @DisplayName("with a zero delay, should resume the shutdown immediately and silently")
    void shouldResumeImmediatelyWithZeroDelay() {
        MetricsScrapeGracePeriod gracePeriod = gracePeriod(Duration.ZERO);
        gracePeriod.start();
        AtomicBoolean resumed = new AtomicBoolean(false);

        gracePeriod.stop(() -> resumed.set(true));

        assertThat(resumed).isTrue();
        assertThat(requestedDelays).isEmpty();
        assertThat(logs.list).isEmpty();
    }

    @Test
    @DisplayName("should resume the shutdown only once the delay has elapsed")
    void shouldResumeAfterDelay() {
        MetricsScrapeGracePeriod gracePeriod = gracePeriod(Duration.ofSeconds(75));
        gracePeriod.start();
        AtomicBoolean resumed = new AtomicBoolean(false);

        gracePeriod.stop(() -> resumed.set(true));

        // stop() returns without blocking the shutdown thread, the callback being delayed
        assertThat(resumed).isFalse();
        assertThat(requestedDelays).containsExactly(Duration.ofSeconds(75));
        assertThat(delayedTasks).hasSize(1);

        delayedTasks.getFirst().run();
        assertThat(resumed).isTrue();
    }

    @Test
    @DisplayName("should log the start and the end of the wait, with the dispatch totals")
    void shouldLogWaitWithTotals() {
        meterRegistry.counter(DISPATCHED_MESSAGE, "sender", "fr.health.samuA").increment(3);
        meterRegistry.counter(DISPATCHED_MESSAGE, "sender", "fr.health.samuB").increment(4);
        meterRegistry.counter(DISPATCH_ERROR, "reason", "INVALID_MESSAGE").increment(2);
        MetricsScrapeGracePeriod gracePeriod = gracePeriod(Duration.ofSeconds(75));
        gracePeriod.start();

        gracePeriod.stop(() -> {});
        assertThat(logMessages()).singleElement().asString().contains("75s");

        delayedTasks.getFirst().run();
        assertThat(logMessages())
                .hasSize(2)
                .last()
                .asString()
                .contains("75s", "dispatch.message=7", "dispatch.error=2");
    }

    @Test
    @DisplayName("should log zero totals when no message has been dispatched")
    void shouldLogZeroTotalsWithoutCounters() {
        MetricsScrapeGracePeriod gracePeriod = gracePeriod(Duration.ofSeconds(75));
        gracePeriod.start();

        gracePeriod.stop(() -> {});
        delayedTasks.getFirst().run();

        assertThat(logMessages())
                .last()
                .asString()
                .contains("dispatch.message=0", "dispatch.error=0");
    }

    @Test
    @DisplayName("should resume the shutdown even if logging the totals fails")
    void shouldResumeEvenIfTotalsFail() {
        // a registry that throws when searched, standing for any unexpected failure
        MetricsScrapeGracePeriod gracePeriod =
                new MetricsScrapeGracePeriod(
                        Duration.ofSeconds(75),
                        PHASE_TIMEOUT,
                        null,
                        d -> (Executor) delayedTasks::add);
        gracePeriod.start();
        AtomicBoolean resumed = new AtomicBoolean(false);

        gracePeriod.stop(() -> resumed.set(true));

        assertThatThrownBy(() -> delayedTasks.getFirst().run())
                .isInstanceOf(NullPointerException.class);
        assertThat(resumed).isTrue();
    }

    @Test
    @DisplayName("should really wait with the default scheduling")
    void shouldReallyWaitWithDefaultScheduling() {
        MetricsScrapeGracePeriod gracePeriod =
                new MetricsScrapeGracePeriod(Duration.ofMillis(300), PHASE_TIMEOUT, meterRegistry);
        gracePeriod.start();
        AtomicBoolean resumed = new AtomicBoolean(false);
        long start = System.nanoTime();

        gracePeriod.stop(() -> resumed.set(true));

        assertThat(resumed).isFalse();
        await().atMost(Duration.ofSeconds(5)).untilTrue(resumed);
        assertThat(Duration.ofNanos(System.nanoTime() - start))
                .isGreaterThanOrEqualTo(Duration.ofMillis(300));
    }

    @Test
    @DisplayName("should warn when the phase timeout does not exceed the delay")
    void shouldWarnWhenPhaseTimeoutDoesNotExceedDelay() {
        gracePeriod(Duration.ofSeconds(75), Duration.ofSeconds(75));

        assertThat(logs.list)
                .singleElement()
                .satisfies(
                        event -> {
                            assertThat(event.getLevel()).isEqualTo(Level.WARN);
                            assertThat(event.getFormattedMessage())
                                    .contains("spring.lifecycle.timeout-per-shutdown-phase", "75s");
                        });
    }

    @Test
    @DisplayName("should not warn about the phase timeout with a zero delay")
    void shouldNotWarnWithZeroDelay() {
        gracePeriod(Duration.ZERO, Duration.ZERO);

        assertThat(logs.list).isEmpty();
    }

    @Test
    @DisplayName("should refuse a negative delay")
    void shouldRefuseNegativeDelay() {
        assertThatThrownBy(() -> gracePeriod(Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dispatcher.shutdown.metrics-scrape-delay");
    }
}
