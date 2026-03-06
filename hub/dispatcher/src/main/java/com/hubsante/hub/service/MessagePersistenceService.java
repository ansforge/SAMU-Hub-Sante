package com.hubsante.hub.service;

import static com.hubsante.hub.config.Constants.HEALTH_VHOST_PREFIX;
import static com.hubsante.hub.config.Constants.NEXSIS_VHOST;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubsante.hub.config.LogConstants;
import com.hubsante.hub.config.StructuredLogger;
import com.hubsante.hub.model.Message;
import com.hubsante.hub.repository.MessageRepository;
import com.hubsante.hub.utils.EdxlUtils;
import com.hubsante.model.EdxlHandler;
import com.hubsante.model.edxl.EdxlMessage;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MessagePersistenceService {

    /**
     * Messages to persist when circulating from 18 → 15 (vhost 15-nexsis).
     */
    private static final Set<String> NEXSIS_PERSISTED_USE_CASES =
            Set.of("ResourcesInfoCisuWrapper");

    /**
     * Messages to persist when circulating from 15 → 18 (vhost 15-15_v*).
     */
    private static final Set<String> HEALTH_PERSISTED_USE_CASES =
            Set.of("ResourcesInfoWrapper", "ResourcesStatusWrapper");

    private final MessageRepository repository;
    private final EdxlHandler edxlHandler;
    private final ObjectMapper objectMapper;
    private static final StructuredLogger structuredLog = new StructuredLogger(log);

    public MessagePersistenceService(
            MessageRepository repository,
            EdxlHandler edxlHandler,
            @Qualifier("jsonMapper") ObjectMapper objectMapper) {
        this.repository = repository;
        this.edxlHandler = edxlHandler;
        this.objectMapper = objectMapper;
    }

    /**
     * Persists the original message if the (vhost, useCase) pair matches a defined persistence case.
     *
     * @param edxlMessage original message (before conversion)
     * @param vhost source vhost, used to determine the direction of circulation
     */
    public void persistIfRequired(EdxlMessage edxlMessage, String vhost) {
        String useCase = EdxlUtils.getUseCaseFromMessage(edxlMessage.getFirstContentMessage());

        boolean shouldPersist = shouldPersist(vhost, useCase);
        if (!shouldPersist) {
            return;
        }

        try {
            String jsonEdxl = edxlHandler.serializeJsonEDXL(edxlMessage);
            Map<String, Object> payload =
                    objectMapper.readValue(jsonEdxl, new TypeReference<>() {});

            Message doc =
                    Message.builder()
                            .type(useCase)
                            .payload(payload)
                            .build();

            repository.save(doc);

            structuredLog.info(
                    String.format(
                            "Message of type %s persisted from vhost %s", useCase, vhost),
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
        }
    }

    /**
     * Determines if a message should be persisted based on vhost and useCase.
     *
     * @param vhost the vhost name
     * @param useCase the use case extracted from the message
     * @return true if the message should be persisted, false otherwise
     */
    private boolean shouldPersist(String vhost, String useCase) {
        if (NEXSIS_VHOST.equals(vhost)) {
            return NEXSIS_PERSISTED_USE_CASES.contains(useCase);
        }
        if (vhost != null && vhost.startsWith(HEALTH_VHOST_PREFIX)) {
            return HEALTH_PERSISTED_USE_CASES.contains(useCase);
        }
        return false;
    }
}
