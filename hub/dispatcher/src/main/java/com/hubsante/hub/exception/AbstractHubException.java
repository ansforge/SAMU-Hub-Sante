package com.hubsante.hub.exception;

import com.hubsante.model.report.ErrorCode;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

public abstract class AbstractHubException extends AmqpRejectAndDontRequeueException {

    private ErrorCode errorCode;
    private String referencedDistributionID;
    public AbstractHubException(String message, ErrorCode errorCode, String referencedDistributionID) {
        super(message);
        this.errorCode = errorCode;
        this.referencedDistributionID = referencedDistributionID;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getReferencedDistributionID() { return referencedDistributionID; }
}
