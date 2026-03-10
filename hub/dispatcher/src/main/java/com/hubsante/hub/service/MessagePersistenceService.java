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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubsante.hub.config.HubConfiguration;
import com.hubsante.hub.config.LogConstants;
import com.hubsante.hub.config.StructuredLogger;
import com.hubsante.hub.exception.HubPersistenceException;
import com.hubsante.hub.model.PersistedMessage;
import com.hubsante.hub.repository.MessageRepository;
import com.hubsante.hub.utils.EdxlUtils;
import com.hubsante.model.EdxlHandler;
import com.hubsante.model.edxl.EdxlMessage;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MessagePersistenceService {

    private final MessageRepository repository;
    private final EdxlHandler edxlHandler;
    private final ObjectMapper objectMapper;
    private final HubConfiguration hubConfig;
    private static final StructuredLogger structuredLog = new StructuredLogger(log);

    public MessagePersistenceService(
            MessageRepository repository,
            EdxlHandler edxlHandler,
            @Qualifier("jsonMapper") ObjectMapper objectMapper,
            HubConfiguration hubConfig) {
        this.repository = repository;
        this.edxlHandler = edxlHandler;
        this.objectMapper = objectMapper;
        this.hubConfig = hubConfig;
    }

    public void persist(EdxlMessage edxlMessage) throws HubPersistenceException {
        String useCase = EdxlUtils.getUseCaseFromMessage(edxlMessage.getFirstContentMessage());
        String vhost = hubConfig.getVhost();
        try {
            String jsonEdxl = edxlHandler.serializeJsonEDXL(edxlMessage);
            Map<String, Object> payload =
                    objectMapper.readValue(jsonEdxl, new TypeReference<>() {});

            PersistedMessage doc = PersistedMessage.builder().type(useCase).payload(payload).build();

            repository.save(doc);

            structuredLog.info(
                    String.format("Message of type %s persisted from vhost %s", useCase, vhost),
                    Map.of(
                            LogConstants.DISTRIBUTION_ID,
                            edxlMessage.getDistributionID(),
                            LogConstants.MESSAGE_TYPE,
                            useCase));
        } catch (Exception e) {
            structuredLog.error(
                    String.format(
                            "Failed to persist message of type %s from vhost %s", useCase, vhost),
                    Map.of(
                            LogConstants.DISTRIBUTION_ID,
                            edxlMessage.getDistributionID(),
                            LogConstants.MESSAGE_TYPE,
                            useCase),
                    e);
            throw new HubPersistenceException(e.getMessage(), edxlMessage.getDistributionID());
        }
    }
}
