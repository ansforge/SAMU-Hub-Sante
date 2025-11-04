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
