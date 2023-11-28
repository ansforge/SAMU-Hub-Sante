package com.hubsante.hub.exception;

import com.hubsante.model.report.ErrorCode;

public class NotAllowedContentTypeException extends AbstractHubException {
    public NotAllowedContentTypeException(String message, String referencedDistributionID) {
        super(message, ErrorCode.NOT_ALLOWED_CONTENT_TYPE, referencedDistributionID);
    }
}
