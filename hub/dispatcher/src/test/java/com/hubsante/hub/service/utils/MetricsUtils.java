/**
 * Copyright © 2023-2024 Agence du Numerique en Sante (ANS)
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
package com.hubsante.hub.service.utils;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.search.Search;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static com.hubsante.hub.config.Constants.DISPATCH_ERROR;

public class MetricsUtils {

    public static double getCurrentCount(Counter counter) {
        return Objects.requireNonNull(counter).count();
    }

    public static Search targetCounter(MeterRegistry registry, String... tags) {
        return registry.find(DISPATCH_ERROR).tags(tags);
    }

    public static double getOverallCounterForClient(MeterRegistry registry, String sender) {
        AtomicReference<Double> overall = new AtomicReference<>(0.0);
        registry.forEachMeter(meter -> {
            if (meter.getId().getTags().contains(Tag.of("sender", sender))) {
                Counter counter = registry.find(DISPATCH_ERROR).tags(meter.getId().getTags()).counter();
                double meterValue = counter != null ? counter.count() : 0.0;
                overall.set(overall.get() + meterValue);
            }
        });
        return overall.get();
    }
}
