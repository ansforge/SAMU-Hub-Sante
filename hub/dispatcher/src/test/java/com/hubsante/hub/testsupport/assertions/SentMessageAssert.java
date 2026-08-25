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
package com.hubsante.hub.testsupport.assertions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hubsante.model.EdxlHandler;
import com.hubsante.model.edxl.EdxlMessage;
import com.hubsante.model.report.ErrorWrapper;
import java.nio.charset.StandardCharsets;
import org.assertj.core.api.AbstractAssert;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

public class SentMessageAssert extends AbstractAssert<SentMessageAssert, Message> {

    private static final EdxlHandler EDXL = new EdxlHandler();

    SentMessageAssert(Message actual) {
        super(actual, SentMessageAssert.class);
    }

    public String getBodyAsString() {
        isNotNull();
        return new String(actual.getBody(), StandardCharsets.UTF_8);
    }

    private EdxlMessage deserializeEdxl() {
        isNotNull();
        String body = getBodyAsString();
        try {
            return MessageProperties.CONTENT_TYPE_XML.equals(
                            actual.getMessageProperties().getContentType())
                    ? EDXL.deserializeXmlEDXL(body)
                    : EDXL.deserializeJsonEDXL(body);
        } catch (JsonProcessingException e) {
            throw failure("sent message body is not a deserializable EDXL message: %s", body);
        }
    }

    /** Reads the body as an EDXL error report. */
    public ErrorAssert asError() {
        EdxlMessage edxlMessage = deserializeEdxl();
        if (!(edxlMessage.getFirstContentMessage() instanceof ErrorWrapper wrapper)) {
            throw failure(
                    "expected the sent message to carry an ErrorWrapper but was <%s>",
                    edxlMessage.getFirstContentMessage().getClass().getSimpleName());
        }
        return new ErrorAssert(wrapper.getError());
    }
}
