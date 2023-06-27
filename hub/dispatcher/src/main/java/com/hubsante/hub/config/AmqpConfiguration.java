package com.hubsante.hub.config;

import com.rabbitmq.client.DefaultSaslConfig;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.annotation.PostConstruct;

@Configuration
public class AmqpConfiguration {
    public static final String HUBSANTE_EXCHANGE = "hubsante";
    public static final String CONSUME_QUEUE_NAME = "*.out.*";
    public static final String MESSAGE_ROUTING_KEY = "#.out.message";
    public static final String INFO_ROUTING_KEY = "#.out.info";
    public static final String ACK_ROUTING_KEY = "#.out.ack";

    private final CachingConnectionFactory connectionFactory;

    private RabbitProperties rabbitProperties;

    @Autowired
    public void RabbitMQConfig(RabbitProperties rabbitProperties) {
        this.rabbitProperties = rabbitProperties;
    }

    public AmqpConfiguration(CachingConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @PostConstruct
    public void init() {
        // To avoid "No compatible authentication mechanism found - server offered [EXTERNAL]" errors
        // Ref.: https://github.com/spring-projects/spring-boot/issues/6719#issuecomment-259268574
        if (rabbitProperties.getSsl().getEnabled() && rabbitProperties.getSsl().getKeyStore() != null) {
            connectionFactory.getRabbitConnectionFactory().setSaslConfig(DefaultSaslConfig.EXTERNAL);
        }
    }
}
