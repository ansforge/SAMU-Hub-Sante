package com.hubsante.hub.exception;

import com.hubsante.model.report.ErrorCode;

public class SchemaValidationException extends AbstractHubException {

    public SchemaValidationException(String message) {
        super(message, ErrorCode.INVALID_MESSAGE);
    }
}
