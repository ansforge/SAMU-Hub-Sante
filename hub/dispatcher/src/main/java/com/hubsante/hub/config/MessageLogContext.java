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

import static com.hubsante.hub.config.AmqpConfiguration.ORIGINAL_ROUTING_KEY;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;

public final class MessageLogContext {

    public static final Set<LogConstants> CONTEXT_KEYS =
            EnumSet.of(
                    LogConstants.SENDER_ID,
                    LogConstants.RECIPIENT_ID,
                    LogConstants.DISTRIBUTION_ID,
                    LogConstants.MESSAGE_TYPE,
                    LogConstants.ROUTING_KEY,
                    LogConstants.EDITOR,
                    LogConstants.CASE_ID);

    private final Map<LogConstants, String> previousValues = new EnumMap<>(LogConstants.class);

    private MessageLogContext() {
        CONTEXT_KEYS.forEach(key -> previousValues.put(key, MDC.get(key.getKey())));
    }

    public static MessageLogContext open(Message message) {
        MessageLogContext context = new MessageLogContext();
        put(
                LogConstants.ROUTING_KEY,
                message.getMessageProperties().getHeader(ORIGINAL_ROUTING_KEY));
        return context;
    }

    public static void put(LogConstants key, String value) {
        if (!CONTEXT_KEYS.contains(key)) {
            throw new IllegalArgumentException(key + " is not a message log context key");
        }
        if (value != null) {
            MDC.put(key.getKey(), value);
        }
    }

    public void restore() {
        previousValues.forEach(
                (key, value) -> {
                    if (value == null) {
                        MDC.remove(key.getKey());
                    } else {
                        MDC.put(key.getKey(), value);
                    }
                });
    }
}
