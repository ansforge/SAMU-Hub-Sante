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
import static com.hubsante.hub.config.Constants.CASE_ID_SPAN_TAG;
import static com.hubsante.hub.config.Constants.DISTRIBUTION_ID_SPAN_TAG;
import static com.hubsante.hub.config.Constants.EDITOR_SPAN_TAG;

import com.hubsante.hub.config.HubConfiguration;
import com.hubsante.hub.utils.EdxlUtils;
import com.hubsante.hub.utils.MessageUtils;
import com.hubsante.model.edxl.ContentMessage;
import com.hubsante.model.edxl.EdxlEnvelope;
import com.hubsante.model.edxl.EdxlMessage;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ObservabilityService {
    private final HubConfiguration hubConfig;
    private final Tracer tracer;

    public ObservabilityService(HubConfiguration hubConfig, Tracer tracer) {
        this.hubConfig = hubConfig;
        this.tracer = tracer;
    }

    public void tagCurrentSpanWithMessageMetadata(EdxlMessage edxlMessage) {
        Span span = tracer.currentSpan();
        if (span == null) {
            return;
        }
        String sender = edxlMessage.getSenderID();
        ContentMessage contentMessage = edxlMessage.getFirstContentMessage();
        span.tag(SENDER_SPAN_TAG, sender);
        span.tag(RECIPIENT_SPAN_TAG, MessageUtils.getRecipientID(edxlMessage));
        span.tag(USE_CASE_SPAN_TAG, EdxlUtils.getUseCaseFromMessage(contentMessage));
        span.tag(EDITOR_SPAN_TAG, hubConfig.getClientPropertiesRegistry().getClientEditor(sender));
        span.tag(CASE_ID_SPAN_TAG, EdxlUtils.getCaseIdFromMessage(contentMessage));
        span.tag(DISTRIBUTION_ID_SPAN_TAG, edxlMessage.getDistributionID());
    }

    public void tagCurrentSpanWithMinimalMetadata(String distributionId, String sender) {
        Span span = tracer.currentSpan();
        if (span == null) {
            return;
        }
        span.tag(SENDER_SPAN_TAG, sender);
        span.tag(DISTRIBUTION_ID_SPAN_TAG, distributionId);
    }

    public void tagCurrentSpanWithEnvelopeMetadata(EdxlEnvelope envelope) {
        Span span = tracer.currentSpan();
        if (span == null) {
            return;
        }
        String sender = envelope.getSenderID();
        span.tag(SENDER_SPAN_TAG, sender);
        span.tag(
                RECIPIENT_SPAN_TAG,
                envelope.getDescriptor().getExplicitAddress().getExplicitAddressValue());
        span.tag(EDITOR_SPAN_TAG, hubConfig.getClientPropertiesRegistry().getClientEditor(sender));
        span.tag(DISTRIBUTION_ID_SPAN_TAG, envelope.getDistributionID());
    }
}
