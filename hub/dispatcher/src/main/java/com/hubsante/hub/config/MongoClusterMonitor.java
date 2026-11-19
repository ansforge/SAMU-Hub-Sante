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

import com.mongodb.event.ClusterDescriptionChangedEvent;
import com.mongodb.event.ClusterListener;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Tracks MongoDB availability from the driver's own background cluster monitoring, without
 * querying the database. The database is considered up as long as the cluster has a writable
 * server (a replica set without a primary, e.g. during an election, is down).
 */
@Component
@Slf4j
public class MongoClusterMonitor implements ClusterListener {

    // false until the first cluster description reporting a writable server
    private final AtomicBoolean up = new AtomicBoolean(false);

    public MongoClusterMonitor(MeterRegistry meterRegistry) {
        Gauge.builder("dispatcher_mongodb_up", up, flag -> flag.get() ? 1 : 0)
                .description("1 if the MongoDB cluster has a writable server, 0 otherwise")
                .register(meterRegistry);
    }

    public boolean isUp() {
        return up.get();
    }

    @Override
    public void clusterDescriptionChanged(ClusterDescriptionChangedEvent event) {
        boolean nowUp = event.getNewDescription().hasWritableServer();
        boolean wasUp = up.getAndSet(nowUp);
        if (nowUp && !wasUp) {
            log.info("[MongoDB] Connection restored");
        } else if (!nowUp && wasUp) {
            log.error(
                    "[MongoDB] Connection lost: {}",
                    event.getNewDescription().getShortDescription());
        }
    }
}
