package com.hubsante.hub.exception;

import com.hubsante.model.report.ErrorCode;

public class SenderInconsistencyException extends AbstractHubException {
    public SenderInconsistencyException(String message, String referencedDistributionID) {
        super(message, ErrorCode.SENDER_INCONSISTENCY, referencedDistributionID);
    }
}
