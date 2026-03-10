package com.hubsante.hub.exception;

import com.hubsante.model.report.ErrorCode;

public class HubPersistenceException extends AbstractHubException {
    public HubPersistenceException(String message, String referencedDistributionId) {
        super(
                "Error during internal call to Hub Santé persistence service: " + message,
                ErrorCode.INVALID_MESSAGE,
                referencedDistributionId);
    }

    public HubPersistenceException(
            String message,
            String referencedDistributionId,
            String recipientId,
            String messageType) {
        super(
                "Error during internal call to Hub Santé persistence service: " + message,
                ErrorCode.INVALID_MESSAGE,
                referencedDistributionId,
                recipientId,
                messageType);
    }
}
