package com.hubsante.hub.exception;

import com.hubsante.model.report.ErrorCode;

public class UnroutableMessageException extends  AbstractHubException {
    public UnroutableMessageException(String message, String referencedDistributionID) {
        super(message, ErrorCode.UNROUTABLE_MESSAGE, referencedDistributionID);
    }
}
