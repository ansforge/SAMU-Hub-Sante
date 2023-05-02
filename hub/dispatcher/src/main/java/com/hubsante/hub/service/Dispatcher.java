package com.hubsante.hub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hubsante.model.cisu.AddresseeType;
import com.hubsante.model.cisu.BasicMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.hubsante.hub.config.AmqpConfiguration.CONSUME_QUEUE_NAME;

@Service
@Slf4j
public class Dispatcher {

    private final RabbitTemplate rabbitTemplate;

    public Dispatcher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = CONSUME_QUEUE_NAME)
    public void dispatch(Message message) {

        String receivedRoutingKey = message.getMessageProperties().getReceivedRoutingKey();
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        BasicMessage basicMessage;
        try {
            basicMessage = mapper.readValue(message.getBody(), BasicMessage.class);
            log.info(" [x] Received '" + receivedRoutingKey + "':" + basicMessage);
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

        // TODO (bbo): migrate with edxl envelope
        List<String> recipients = new ArrayList<>();
        for (AddresseeType recipient : basicMessage.getRecipients().getRecipient()) {
            recipients.add(recipient.getName());
        }

        for (String recipient : recipients) {
            //TODO (bbo) : get msgType from headers : CISU enum doesn't contains INFO ?
            log.info("msg type : " + basicMessage.getMsgType().getValue());

            String queueType = basicMessage.getMsgType().getValue().equals("ACK") ? "ack" : "message";
            String queueName = recipient + ".in." + queueType;

            Message forwardedMsg = new Message(message.getBody(), message.getMessageProperties());
            try {
                rabbitTemplate.send("", queueName, forwardedMsg);
                log.info("  ↳ [x] Sent '" + queueName + "':" + new String(forwardedMsg.getBody(), StandardCharsets.UTF_8));
            } catch (AmqpException e) {
                // TODO (bbo) : if we catch an AmqpException, ii won't be retried.
                //  We should instead define a retry strategy.
                log.error("[ERROR] Failed to dispatch message " + basicMessage + ". Raised exception: " + e);
            }
        }
    }
}
