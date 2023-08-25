package com.hubsante.hub.exception;

import com.hubsante.model.report.ErrorCode;

public class UnrecognizedMessageFormatException extends AbstractHubException {
    public UnrecognizedMessageFormatException(String message) {
        super(message, ErrorCode.UNRECOGNIZED_MESSAGE_FORMAT);
    }
}
