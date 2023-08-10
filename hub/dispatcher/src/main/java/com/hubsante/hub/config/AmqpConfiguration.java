package com.hubsante.hub.config;

import com.rabbitmq.client.DefaultSaslConfig;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
public class AmqpConfiguration {
    public static final String HUBSANTE_EXCHANGE = "hubsante";
    public static final String DISTRIBUTION_EXCHANGE = "distribution";
    public static final String DISPATCH_QUEUE_NAME = "dispatch";
    public static final String DISPATCH_DLQ_NAME = "dispatch.dlq";
    public static final String DLQ_REASON = "x-first-death-reason";
    public static final String DLQ_MESSAGE_ORIGIN = "x-first-death-queue";

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
