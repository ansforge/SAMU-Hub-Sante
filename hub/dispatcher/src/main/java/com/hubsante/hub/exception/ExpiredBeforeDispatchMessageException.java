package com.hubsante.hub.exception;

import com.hubsante.model.report.ErrorCode;

public class ExpiredBeforeDispatchMessageException extends AbstractHubException {
    public ExpiredBeforeDispatchMessageException(String message) {
        super(message, ErrorCode.EXPIRED_MESSAGE_BEFORE_ROUTING);
    }
}
