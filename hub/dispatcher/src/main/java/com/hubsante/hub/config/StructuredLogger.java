/**
 * Copyright © 2023-2025 Agence du Numerique en Sante (ANS)
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


import org.slf4j.Logger;
import org.slf4j.event.Level;
import org.slf4j.spi.LoggingEventBuilder;

import java.util.Map;

public record StructuredLogger(Logger logger) {

    public void log(Level level, String message, Map<LogConstants, String> metadata, Throwable t) {
        LoggingEventBuilder builder = logger.atLevel(level);
        if (metadata != null) {
            metadata.forEach((lc, v) -> builder.addKeyValue(lc.getKey(), v));
        }
        if (t != null) {
            builder.setCause(t);
        }
        builder.log(message);
    }

    public void info(String message, Map<LogConstants, String> metadata) {
        log(Level.INFO, message, metadata, null);
    }

    public void debug(String message, Map<LogConstants, String> metadata) {
        log(Level.DEBUG, message, metadata, null);
    }

    public void warn(String message, Map<LogConstants, String> metadata) {
        log(Level.WARN, message, metadata, null);
    }

    public void warn(String message, Map<LogConstants, String> metadata, Throwable t) {
        log(Level.WARN, message, metadata, t);
    }

    public void error(String message, Map<LogConstants, String> metadata) {
        log(Level.ERROR, message, metadata, null);
    }

    public void error(String message, Map<LogConstants, String> metadata, Throwable t) {
        log(Level.ERROR, message, metadata, t);
    }
}
