package com.hubsante.hub.exception;

import com.hubsante.model.report.ErrorCode;

public class DeadLetteredMessageException extends AbstractHubException {
    public DeadLetteredMessageException(String message) {
        super(message, ErrorCode.DEAD_LETTER_QUEUED);
    }
}
