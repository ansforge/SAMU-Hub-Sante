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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubsante.hub.config.LogConstants;
import com.hubsante.hub.config.StructuredLogger;
import com.hubsante.hub.exception.ConversionException;
import com.hubsante.hub.utils.ConversionRulesCommand;
import com.hubsante.hub.utils.EdxlUtils;
import com.hubsante.hub.utils.MessageUtils;
import com.hubsante.model.EdxlHandler;
import com.hubsante.model.edxl.EdxlMessage;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
@Slf4j
public class ConversionHandler {

    private final WebClient conversionWebClient;
    private final EdxlHandler edxlHandler;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final StructuredLogger structuredLog = new StructuredLogger(log);

    public ConversionHandler(WebClient conversionWebClient, EdxlHandler edxlHandler) {
        this.conversionWebClient = conversionWebClient;
        this.edxlHandler = edxlHandler;
    }

    protected String applyConversionRules(ConversionRulesCommand applyConversionRulesCommand)
            throws JsonProcessingException {
        String sourceModelVersion = applyConversionRulesCommand.getSourceModelVersion();
        String targetModelVersion = applyConversionRulesCommand.getTargetModelVersion();
        Boolean isCisuConversion = applyConversionRulesCommand.getCisuConversion();
        EdxlMessage edxlMessage = applyConversionRulesCommand.getEdxlMessage();
        String distributionId = edxlMessage.getDistributionID();
        String senderId = edxlMessage.getSenderID();
        String recipientId = MessageUtils.getRecipientID(edxlMessage);
        String messageType = EdxlUtils.getUseCaseFromMessage(edxlMessage.getFirstContentMessage());

        String jsonEdxlString = edxlHandler.serializeJsonEDXL(edxlMessage);

        try {
            structuredLog.debug(
                    String.format(
                            "Starting conversion from %s to %s, isCisu ? %s",
                            sourceModelVersion, targetModelVersion, isCisuConversion),
                    Map.of(
                            LogConstants.DISTRIBUTION_ID,
                            distributionId,
                            LogConstants.SENDER_ID,
                            senderId,
                            LogConstants.RECIPIENT_ID,
                            recipientId,
                            LogConstants.MESSAGE_TYPE,
                            messageType));
            String convertedJson =
                    callConversionService(
                            jsonEdxlString,
                            sourceModelVersion,
                            targetModelVersion,
                            isCisuConversion,
                            distributionId);
            structuredLog.debug(
                    "Message converted successfully: " + convertedJson,
                    Map.of(
                            LogConstants.DISTRIBUTION_ID,
                            distributionId,
                            LogConstants.SENDER_ID,
                            senderId,
                            LogConstants.RECIPIENT_ID,
                            recipientId,
                            LogConstants.MESSAGE_TYPE,
                            messageType));

            return convertedJson; // returns a string (deserialization is not possible because of
            // version change)
        } catch (RuntimeException e) {
            // Error raised by the conversion service or its call
            structuredLog.error(
                    "Error during internal call to Hub Santé conversion service " + e,
                    Map.of(
                            LogConstants.DISTRIBUTION_ID,
                            distributionId,
                            LogConstants.SENDER_ID,
                            senderId,
                            LogConstants.RECIPIENT_ID,
                            recipientId,
                            LogConstants.MESSAGE_TYPE,
                            messageType));
            throw new ConversionException(e.getMessage(), distributionId, recipientId, messageType);
        }
    }

    protected String callConversionService(
            String jsonEdxlString,
            String sourceVersion,
            String targetVersion,
            boolean cisuConversion,
            String distributionId) {
        // Create request body with all required parameters
        String requestBody =
                String.format(
                        "{\"edxl\": %s, \"sourceVersion\": \"%s\", \"targetVersion\": \"%s\", \"cisuConversion\": %s}",
                        jsonEdxlString, sourceVersion, targetVersion, cisuConversion);

        try {
            String response =
                    conversionWebClient
                            .post()
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
                String errorMessage =
                        errorNode.has("error") ? errorNode.get("error").asText() : e.getMessage();
                structuredLog.error(
                        "Error received from converter service " + errorMessage,
                        Map.of(LogConstants.DISTRIBUTION_ID, distributionId));
                throw new ConversionException(errorMessage, distributionId);
            } catch (JsonProcessingException jsonException) {
                // this should never happen as long as the objectMapper.readTree method has already
                // been called earlier in the 'try' block
                throw new ConversionException(
                        "Failed to parse error response from conversion service: " + e.getMessage(),
                        distributionId);
            }
        } catch (JsonProcessingException e) {
            structuredLog.error(
                    "Failed to parse error response from conversion service: " + e.getMessage(),
                    Map.of(LogConstants.DISTRIBUTION_ID, distributionId));
            throw new ConversionException(
                    "Failed to parse response from conversion service: " + e.getMessage(),
                    distributionId);
        }
    }
}
