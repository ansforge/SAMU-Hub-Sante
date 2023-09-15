package com.hubsante.hub.exception;

import com.hubsante.model.report.ErrorCode;

public class DeliveryModeInconsistencyException extends AbstractHubException {

    public DeliveryModeInconsistencyException(String message) {
        super(message, ErrorCode.DELIVERY_MODE_INCONSISTENCY);
    }
}
