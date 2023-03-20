package com.hubsante.hub.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AmqpConfiguration {

    public static final String HUBSANTE_EXCHANGE = "hubsante";
    public static final String CONSUME_QUEUE_NAME = "*.out.*";
    public static final String MESSAGE_ROUTING_KEY = "*.out.message";
    public static final String INFO_ROUTING_KEY = "*.out.info";
    public static final String ACK_ROUTING_KEY = "*.out.ack";

    private final ConnectionFactory connectionFactory;

    public AmqpConfiguration(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Bean
    public AmqpAdmin amqpAdmin() {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    TopicExchange hubsanteExchange() {
        return new TopicExchange(HUBSANTE_EXCHANGE);
    }

    @Bean
    Queue outMessageQueue() {
        return QueueBuilder.durable(CONSUME_QUEUE_NAME).build();
    }

    @Bean
    Binding outMessageBinding() {
        return BindingBuilder.bind(outMessageQueue()).to(hubsanteExchange()).with(MESSAGE_ROUTING_KEY);
    }

    @Bean
    Binding outInfoBinding() {
        return BindingBuilder.bind(outMessageQueue()).to(hubsanteExchange()).with(INFO_ROUTING_KEY);
    }

    @Bean
    Binding outAckBinding() {
        return BindingBuilder.bind(outMessageQueue()).to(hubsanteExchange()).with(ACK_ROUTING_KEY);
    }
}
