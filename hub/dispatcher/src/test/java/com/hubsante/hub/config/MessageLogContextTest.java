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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

@DisplayName("MessageLogContext")
class MessageLogContextTest {

    private static final String ROUTING_KEY = "fr.health.samuA";

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    private static Message message(String originalRoutingKey) {
        MessageProperties properties = new MessageProperties();
        if (originalRoutingKey != null) {
            properties.setHeader(ORIGINAL_ROUTING_KEY, originalRoutingKey);
        }
        return new Message(new byte[0], properties);
    }

    @Test
    @DisplayName("open puts the original routing key")
    void openPutsRoutingKey() {
        MessageLogContext.open(message(ROUTING_KEY));

        assertThat(MDC.get(LogConstants.ROUTING_KEY.getKey())).isEqualTo(ROUTING_KEY);
    }

    @Test
    @DisplayName("open without original routing key header leaves the field out")
    void openWithoutHeader() {
        MessageLogContext.open(message(null));

        assertThat(MDC.get(LogConstants.ROUTING_KEY.getKey())).isNull();
    }

    @Test
    @DisplayName("put ignores null values and keeps the known value")
    void putIgnoresNull() {
        MessageLogContext.open(message(ROUTING_KEY));

        MessageLogContext.put(LogConstants.DISTRIBUTION_ID, "regex-id");
        MessageLogContext.put(LogConstants.DISTRIBUTION_ID, null);
        assertThat(MDC.get(LogConstants.DISTRIBUTION_ID.getKey())).isEqualTo("regex-id");

        MessageLogContext.put(LogConstants.DISTRIBUTION_ID, "real-id");
        assertThat(MDC.get(LogConstants.DISTRIBUTION_ID.getKey())).isEqualTo("real-id");
    }

    @Test
    @DisplayName("put rejects keys that are not context keys")
    void putRejectsNonContextKey() {
        assertThatThrownBy(() -> MessageLogContext.put(LogConstants.ERROR_CODE, "code"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("restore removes the context keys and keeps other MDC entries")
    void restoreRemovesContextKeysOnly() {
        MDC.put("traceId", "trace");
        MessageLogContext logContext = MessageLogContext.open(message(ROUTING_KEY));
        MessageLogContext.put(LogConstants.SENDER_ID, "sender");
        MessageLogContext.put(LogConstants.CASE_ID, "case");
        MDC.put("spanId", "span");

        logContext.restore();

        assertThat(MDC.getCopyOfContextMap())
                .containsOnlyKeys("traceId", "spanId")
                .containsEntry("traceId", "trace")
                .containsEntry("spanId", "span");
    }

    @Test
    @DisplayName("restore puts back values set before the scope was opened")
    void restorePutsBackPreviousValues() {
        MessageLogContext.open(message("outer"));
        MessageLogContext.put(LogConstants.SENDER_ID, "outer-sender");
        MessageLogContext inner = MessageLogContext.open(message("inner"));
        MessageLogContext.put(LogConstants.SENDER_ID, "inner-sender");
        MessageLogContext.put(LogConstants.EDITOR, "inner-editor");

        inner.restore();

        assertThat(MDC.get(LogConstants.ROUTING_KEY.getKey())).isEqualTo("outer");
        assertThat(MDC.get(LogConstants.SENDER_ID.getKey())).isEqualTo("outer-sender");
        assertThat(MDC.get(LogConstants.EDITOR.getKey())).isNull();
    }
}
