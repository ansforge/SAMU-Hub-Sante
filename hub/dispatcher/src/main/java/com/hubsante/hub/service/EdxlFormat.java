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

import static com.hubsante.model.config.Constants.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hubsante.hub.utils.MessageUtils;
import com.hubsante.model.EdxlHandler;
import com.hubsante.model.Validator;
import com.hubsante.model.edxl.EdxlEnvelope;
import com.hubsante.model.edxl.EdxlMessage;
import com.hubsante.model.exception.ValidationException;
import java.io.IOException;
import java.util.Optional;
import org.springframework.amqp.core.Message;

enum EdxlFormat {
    JSON(FULL_SCHEMA, ENVELOPE_SCHEMA) {
        @Override
        void validate(Validator validator, String body, String schema)
                throws IOException, ValidationException {
            validator.validateJSON(body, schema);
        }

        @Override
        EdxlMessage deserialize(EdxlHandler edxlHandler, String body)
                throws JsonProcessingException {
            return edxlHandler.deserializeJsonEDXL(body);
        }

        @Override
        EdxlEnvelope deserializeEnvelope(EdxlHandler edxlHandler, String body)
                throws JsonProcessingException {
            return edxlHandler.deserializeJsonEDXLEnvelope(body);
        }
    },
    XML(FULL_XSD, ENVELOPE_XSD) {
        @Override
        void validate(Validator validator, String body, String schema)
                throws IOException, ValidationException {
            validator.validateXML(body, schema);
        }

        @Override
        EdxlMessage deserialize(EdxlHandler edxlHandler, String body)
                throws JsonProcessingException {
            return edxlHandler.deserializeXmlEDXL(body);
        }

        @Override
        EdxlEnvelope deserializeEnvelope(EdxlHandler edxlHandler, String body)
                throws JsonProcessingException {
            return edxlHandler.deserializeXmlEDXLEnvelope(body);
        }
    };

    final String fullSchema;
    final String envelopeSchema;

    EdxlFormat(String fullSchema, String envelopeSchema) {
        this.fullSchema = fullSchema;
        this.envelopeSchema = envelopeSchema;
    }

    abstract void validate(Validator validator, String body, String schema)
            throws IOException, ValidationException;

    abstract EdxlMessage deserialize(EdxlHandler edxlHandler, String body)
            throws JsonProcessingException;

    abstract EdxlEnvelope deserializeEnvelope(EdxlHandler edxlHandler, String body)
            throws JsonProcessingException;

    static Optional<EdxlFormat> of(Message message) {
        if (MessageUtils.isJSON(message)) {
            return Optional.of(JSON);
        }
        if (MessageUtils.isXML(message)) {
            return Optional.of(XML);
        }
        return Optional.empty();
    }
}
