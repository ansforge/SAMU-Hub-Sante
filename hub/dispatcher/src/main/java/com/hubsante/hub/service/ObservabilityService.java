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
package com.hubsante.hub.service;

import static com.hubsante.hub.config.Constants.*;

import com.hubsante.hub.config.HubConfiguration;
import com.hubsante.hub.config.LogConstants;
import com.hubsante.hub.config.MessageLogContext;
import com.hubsante.hub.utils.EdxlUtils;
import com.hubsante.hub.utils.MessageUtils;
import com.hubsante.model.edxl.ContentMessage;
import com.hubsante.model.edxl.EdxlEnvelope;
import com.hubsante.model.edxl.EdxlMessage;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.springframework.stereotype.Component;

@Component
public class ObservabilityService {
    private final HubConfiguration hubConfig;
    private final Tracer tracer;

    public ObservabilityService(HubConfiguration hubConfig, Tracer tracer) {
        this.hubConfig = hubConfig;
        this.tracer = tracer;
    }

    /**
     * Binds the values known before deserialization: the regex-extracted distributionId goes to the
     * log context, and the span is tagged with it and the received routing key as sender.
     */
    public void bindExtractedMetadata(String distributionId, String receivedRoutingKey) {
        MessageLogContext.put(LogConstants.DISTRIBUTION_ID, distributionId);

        Span span = tracer.currentSpan();
        if (span == null) {
            return;
        }
        span.tag(SENDER_SPAN_TAG, receivedRoutingKey);
        span.tag(DISTRIBUTION_ID_SPAN_TAG, distributionId);
    }

    /** Binds the envelope values to the log context and tags the current span with them. */
    public void bind(EdxlEnvelope envelope) {
        String sender = envelope.getSenderID();
        String recipient = MessageUtils.getRecipientID(envelope);
        String editor = hubConfig.getClientPropertiesRegistry().getClientEditor(sender);
        String distributionId = envelope.getDistributionID();

        MessageLogContext.put(LogConstants.SENDER_ID, sender);
        MessageLogContext.put(LogConstants.RECIPIENT_ID, recipient);
        MessageLogContext.put(LogConstants.EDITOR, editor);
        MessageLogContext.put(LogConstants.DISTRIBUTION_ID, distributionId);

        Span span = tracer.currentSpan();
        if (span == null) {
            return;
        }
        span.tag(SENDER_SPAN_TAG, sender);
        span.tag(RECIPIENT_SPAN_TAG, recipient);
        span.tag(EDITOR_SPAN_TAG, editor);
        span.tag(DISTRIBUTION_ID_SPAN_TAG, distributionId);
    }

    /** Binds the envelope and content values to the log context and tags the current span with them. */
    public void bind(EdxlMessage edxlMessage) {
        bind((EdxlEnvelope) edxlMessage);
        ContentMessage contentMessage = edxlMessage.getFirstContentMessage();
        String useCase = EdxlUtils.getUseCaseFromMessage(contentMessage);
        String caseId = EdxlUtils.getCaseIdFromMessage(contentMessage);

        MessageLogContext.put(LogConstants.MESSAGE_TYPE, useCase);
        MessageLogContext.put(LogConstants.CASE_ID, caseId);

        Span span = tracer.currentSpan();
        if (span == null) {
            return;
        }
        span.tag(USE_CASE_SPAN_TAG, useCase);
        span.tag(CASE_ID_SPAN_TAG, caseId);
    }
}
