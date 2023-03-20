package com.hubsante.hub.service;

import org.json.JSONObject;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

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
    public void dispatch(Message message) {

        String receivedRoutingKey = message.getMessageProperties().getReceivedRoutingKey();
        String messageString = new String(message.getBody(), StandardCharsets.UTF_8);
        System.out.println(" [x] Received '" + receivedRoutingKey + "':'" + messageString + "'");

        try {
            JSONObject obj = new JSONObject(messageString);
            String target = obj.getString("to");
            String suffix = receivedRoutingKey.split("[.]")[2];
            String publishRoutingKey = target + ".in." + suffix;

            Queue queue = new Queue(publishRoutingKey, true, false, false);
            Binding binding = new Binding(publishRoutingKey, Binding.DestinationType.QUEUE, "", publishRoutingKey, null);
            amqpAdmin.declareQueue(queue);
            amqpAdmin.declareBinding(binding);

            rabbitTemplate.send("", publishRoutingKey, message);
            System.out.println("  ↳ [x] Sent '" + publishRoutingKey + "':'" + messageString + "'");

        } catch (Exception e) {
            System.out.println("[ERROR] Failed to dispatch message " + messageString + ". Raised exception: " + e);
        }
    }
}
