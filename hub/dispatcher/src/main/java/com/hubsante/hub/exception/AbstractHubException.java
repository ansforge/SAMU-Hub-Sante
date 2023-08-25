package com.hubsante.hub.exception;

import com.hubsante.model.report.ErrorCode;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

public abstract class AbstractHubException extends AmqpRejectAndDontRequeueException {

    private ErrorCode errorCode;
    public AbstractHubException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
