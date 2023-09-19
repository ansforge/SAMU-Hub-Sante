package com.hubsante.hub.exception;

import com.hubsante.model.report.ErrorCode;

public class UndeliveredMessageException extends AbstractHubException {
    public UndeliveredMessageException(String message) {
        super(message, ErrorCode.UNROUTABLE_MESSAGE);
    }
}
