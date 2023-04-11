package com.hubsante.hub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hubsante.message.AddresseeType;
import com.hubsante.message.BasicMessage;
import org.json.JSONObject;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.hubsante.hub.config.AmqpConfiguration.CONSUME_QUEUE_NAME;

@Configuration
public class Dispatcher {

    private final RabbitTemplate rabbitTemplate;
    private final AmqpAdmin amqpAdmin;

    public Dispatcher(RabbitTemplate rabbitTemplate, AmqpAdmin amqpAdmin) {
        this.rabbitTemplate = rabbitTemplate;
        this.amqpAdmin = amqpAdmin;
    }

    @RabbitListener(queues = CONSUME_QUEUE_NAME)
    public void dispatch(Message message) throws IOException {

        String receivedRoutingKey = message.getMessageProperties().getReceivedRoutingKey();
        String messageString = new String(message.getBody(), StandardCharsets.UTF_8);
        System.out.println(" [x] Received '" + receivedRoutingKey + "':'" + messageString + "'");

        try {
            ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
            BasicMessage basicMessage = mapper.readValue(message.getBody(), BasicMessage.class);
            List<String> recipients = new ArrayList<>();
            for (AddresseeType recipient : basicMessage.getRecipients().getRecipient()) {
                System.out.println("recipient : " + recipient.getName());
                recipients.add(recipient.getName());
            }

            for (String recipient : recipients) {
                //TODO : get msgType from headers : CISU enum doesn't contains INFO ?
                System.out.println("msg type : " + basicMessage.getMsgType().getValue());
                String queueType = basicMessage.getMsgType().getValue().equals("ACK") ? "ack" : "message";
                String publishRoutingKey = recipient + ".in." + queueType;
                Queue queue = new Queue(publishRoutingKey, true, false, false);
                Binding binding = new Binding(publishRoutingKey, Binding.DestinationType.QUEUE, "", publishRoutingKey, null);
                amqpAdmin.declareQueue(queue);
                amqpAdmin.declareBinding(binding);

                Message forwardedMsg = new Message(message.getBody(), message.getMessageProperties());
                rabbitTemplate.send("", publishRoutingKey, forwardedMsg);
                System.out.println("  ↳ [x] Sent '" + publishRoutingKey + "':'" + messageString + "'");
            }

        } catch (Exception e) {
            System.out.println("[ERROR] Failed to dispatch message " + messageString + ". Raised exception: " + e);
        }
    }
}
