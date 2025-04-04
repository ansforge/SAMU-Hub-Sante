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
package com.hubsante.hub.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hubsante.hub.exception.ConversionException;
import com.hubsante.model.edxl.EdxlMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@Slf4j
public class ConversionHandler {
    @Autowired
    private WebClient conversionWebClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ConversionHandler(WebClient conversionWebClient) {
        this.conversionWebClient = conversionWebClient;
    }

    protected EdxlMessage convertIncomingCisu(MessageHandler messageHandler, EdxlMessage edxlMessage) throws JsonProcessingException {
        String jsonEdxlString = messageHandler.serializeJsonEDXL(edxlMessage);
        
        try {
            // ToDo: handle the version logic
            String convertedJson = callConversionService(jsonEdxlString, "v3", "v3", true, edxlMessage.getDistributionID());

            log.debug("Successfully converted CISU message");
            return messageHandler.deserializeJsonEDXL(convertedJson);
        } catch (JsonProcessingException e) {
            log.error("Error during CISU message conversion", e);
            throw new ConversionException(e.getMessage(), edxlMessage.getDistributionID());
        }
    }

    protected String callConversionService(String jsonEdxlString, String sourceVersion, String targetVersion, boolean cisuConversion, String distributionID) {
        // Create request body with all required parameters
        String requestBody = String.format(
            "{\"edxl\": %s, \"sourceVersion\": \"%s\", \"targetVersion\": \"%s\", \"cisuConversion\": %s}",
            jsonEdxlString, sourceVersion, targetVersion, cisuConversion
        );

        try {
            String response = conversionWebClient.post()
                    .uri("/convert")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(); // blocking call since the method is not async

            JsonNode jsonNode = objectMapper.readTree(response);
            return jsonNode.get("edxl").toString();

        } catch (WebClientResponseException e) {
            // Handle HTTP error responses from conversion service
            try {
                JsonNode errorNode = objectMapper.readTree(e.getResponseBodyAsString());
                String errorMessage = errorNode.has("error") ? errorNode.get("error").asText() : e.getMessage();
                log.error("Error received from converter service " + errorMessage);
                throw new ConversionException(errorMessage, distributionID);
            } catch (JsonProcessingException jsonException) {
                // this should never happen as long as the objectMapper.readTree method has already been called earlier in the 'try' block
                throw new ConversionException("Failed to parse error response from conversion service: " + e.getMessage(), distributionID);
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse error response from conversion service: " + e.getMessage());
            throw new ConversionException("Failed to parse response from conversion service: " + e.getMessage(), distributionID);
        }
    }
}
