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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * Keeps the metrics endpoint up for a while on shutdown, once the RabbitMQ listeners have stopped,
 * so that Prometheus scrapes the final value of the dispatch counters before the pod goes away.
 *
 * <p>Spring stops the lifecycle beans by decreasing phase: the RabbitMQ listeners ({@code
 * DEFAULT_PHASE}), then this bean, then the web server graceful shutdown ({@code DEFAULT_PHASE -
 * 1024}). The RabbitMQ connection is only closed afterwards, when the beans are destroyed.
 *
 * <p>Sizing rule: delay = scrape interval + scrape timeout + margin (e.g. 60s + 10s + 5s = 75s).
 * {@code spring.lifecycle.timeout-per-shutdown-phase} must exceed the delay, otherwise Spring gives
 * up waiting for this phase (a warning is logged at startup), and so must the Kubernetes
 * termination grace period.
 */
@Component
@Slf4j
public class MetricsScrapeGracePeriod implements SmartLifecycle {

    static final int PHASE = SmartLifecycle.DEFAULT_PHASE - 512;

    private final Duration delay;
    private final MeterRegistry meterRegistry;
    // not a Spring TaskScheduler: being a lifecycle bean of a higher phase, it would already be
    // stopped when this bean waits
    private final Function<Duration, Executor> delayedExecutor;
    private volatile boolean running;

    @Autowired
    public MetricsScrapeGracePeriod(
            @Value("${dispatcher.shutdown.metrics-scrape-delay:0s}") Duration delay,
            // same default as Spring Boot
            @Value("${spring.lifecycle.timeout-per-shutdown-phase:30s}") Duration phaseTimeout,
            MeterRegistry meterRegistry) {
        this(
                delay,
                phaseTimeout,
                meterRegistry,
                d -> CompletableFuture.delayedExecutor(d.toMillis(), TimeUnit.MILLISECONDS));
    }

    MetricsScrapeGracePeriod(
            Duration delay,
            Duration phaseTimeout,
            MeterRegistry meterRegistry,
            Function<Duration, Executor> delayedExecutor) {
        if (delay.isNegative()) {
            throw new IllegalArgumentException(
                    "dispatcher.shutdown.metrics-scrape-delay must not be negative: " + delay);
        }
        // Spring's own timeout message is hidden by the ERROR root log level, and the JVM would
        // then stop before the end of the wait
        if (!delay.isZero() && phaseTimeout.compareTo(delay) <= 0) {
            log.warn(
                    "[Shutdown] spring.lifecycle.timeout-per-shutdown-phase ({}) does not exceed"
                            + " dispatcher.shutdown.metrics-scrape-delay ({}): the shutdown will"
                            + " not wait for the last metrics scrape",
                    format(phaseTimeout),
                    format(delay));
        }
        this.delay = delay;
        this.meterRegistry = meterRegistry;
        this.delayedExecutor = delayedExecutor;
    }

    public Duration getDelay() {
        return delay;
    }

    @Override
    public void start() {
        running = true;
    }

    @Override
    public void stop() {
        running = false;
    }

    /** Resumes the shutdown after the delay, without blocking the shutdown thread meanwhile. */
    @Override
    public void stop(Runnable callback) {
        running = false;
        if (delay.isZero()) {
            callback.run();
            return;
        }
        log.info(
                "[Shutdown] RabbitMQ listeners stopped, keeping the metrics endpoint up for {}",
                format(delay));
        delayedExecutor
                .apply(delay)
                .execute(
                        () -> {
                            try {
                                log.info(
                                        "[Shutdown] End of the {} wait for the last metrics scrape,"
                                                + " totals: {}={}, {}={}",
                                        format(delay),
                                        DISPATCHED_MESSAGE,
                                        total(DISPATCHED_MESSAGE),
                                        DISPATCH_ERROR,
                                        total(DISPATCH_ERROR));
                            } finally {
                                callback.run();
                            }
                        });
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return PHASE;
    }

    // same notation as the property (75s), instead of the ISO-8601 one (PT1M15S)
    private static String format(Duration duration) {
        return duration.toMillisPart() == 0
                ? duration.toSeconds() + "s"
                : duration.toMillis() + "ms";
    }

    // summed over all the label combinations, as Prometheus would with sum()
    private long total(String counterName) {
        return (long)
                meterRegistry.find(counterName).counters().stream()
                        .mapToDouble(Counter::count)
                        .sum();
    }
}
