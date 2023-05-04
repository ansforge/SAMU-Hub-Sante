package com.hubsante.hub.service;

import com.hubsante.model.edxl.DistributionKind;
import com.hubsante.model.edxl.EdxlMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.hubsante.hub.config.AmqpConfiguration.CONSUME_QUEUE_NAME;

@Service
@Slf4j
public class Dispatcher {

    private final RabbitTemplate rabbitTemplate;
    private final EdxlHandler edxlHandler;

    public Dispatcher(RabbitTemplate rabbitTemplate, EdxlHandler edxlHandler) {
        this.rabbitTemplate = rabbitTemplate;
        this.edxlHandler = edxlHandler;
    }

    @RabbitListener(queues = CONSUME_QUEUE_NAME)
    public void dispatch(Message message) {

        String receivedRoutingKey = message.getMessageProperties().getReceivedRoutingKey();
        EdxlMessage edxlMessage;
        String receivedEdxl = new String(message.getBody(), StandardCharsets.UTF_8);
        try {

            edxlMessage = edxlHandler.deserializeJsonEDXL(receivedEdxl);
            log.info(" [x] Received '" + receivedRoutingKey + "':" + receivedEdxl);
        } catch (IOException e) {
            log.error("Could not parse message " + message.getMessageProperties().getMessageId()
                    + "coming from " + message.getMessageProperties().getConsumerQueue());
            // TODO (bbo) : if we end using a "INFO" channel, we should send an INFO message for this type of errors.
            //  if the message is wrongly formatted client-side we should inform the client.
            //  ----
            //  with Spring Rabbit integration, an exception thrown in a @RabbitListener method will end up with message requeuing
            //  by default, except for AmqpRejectAndDontRequeueException which is specially designed for it. Think about moving it to DLQ instead
            throw new AmqpRejectAndDontRequeueException("do not requeue !");
        }

        String queueType = edxlMessage.getDistributionKind().equals(DistributionKind.ACK) ? "ack" : "message";
        String queueName = edxlMessage.getDescriptor().getExplicitAddress().getExplicitAddressValue() + ".in." + queueType;
        Message forwardedMsg = new Message(message.getBody(), message.getMessageProperties());

        try {
            rabbitTemplate.send("", queueName, forwardedMsg);
            log.info("  ↳ [x] Sent '" + queueName + "':" + receivedEdxl);
        } catch (AmqpException e) {
            // TODO (bbo) : if we catch an AmqpException, ii won't be retried.
            //  We should instead define a retry strategy.
            log.error("[ERROR] Failed to dispatch message " + forwardedMsg + ". Raised exception: " + e);
        }
    }
}
