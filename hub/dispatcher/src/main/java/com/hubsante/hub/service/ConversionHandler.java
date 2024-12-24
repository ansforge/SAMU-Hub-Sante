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
package com.hubsante.hub.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hubsante.model.edxl.EdxlMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.MediaType;

@Component
@Slf4j
public class ConversionHandler {
    @Autowired
    private WebClient conversionWebClient;

    public ConversionHandler(WebClient conversionWebClient) {
        this.conversionWebClient = conversionWebClient;
    }

    protected EdxlMessage convertIncomingCisu(MessageHandler messageHandler, EdxlMessage edxlMessage) throws JsonProcessingException {
        String jsonEdxlString = messageHandler.serializeJsonEDXL(edxlMessage);
        
        try {
            String convertedJson = callConversionService(jsonEdxlString);

            log.debug("Successfully converted CISU message");
            return messageHandler.deserializeJsonEDXL(convertedJson);
            
        } catch (Exception e) {
            log.error("Error converting CISU message", e);
            throw new RuntimeException("Failed to convert CISU message", e);
        }
    }

    protected String callConversionService(String jsonEdxlString) {
        return conversionWebClient.post()
                .uri("/convert-cisu")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(jsonEdxlString)
                .retrieve()
                .bodyToMono(String.class)
                .block(); // blocking call since the method is not async
    }
}
