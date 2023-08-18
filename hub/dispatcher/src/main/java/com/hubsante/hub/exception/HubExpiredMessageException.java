package com.hubsante.hub.exception;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;

public class HubExpiredMessageException extends AmqpRejectAndDontRequeueException {
    public HubExpiredMessageException(String message) {
        super(message);
    }
}
