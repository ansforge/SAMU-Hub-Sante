/**
 * Copyright © 2023-2024 Agence du Numerique en Sante (ANS)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hubsante.hub.config;

import com.rabbitmq.client.DefaultSaslConfig;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class AmqpConfiguration {
    public static final String HUBSANTE_EXCHANGE = "hubsante";
    public static final String DISTRIBUTION_EXCHANGE = "distribution";
    public static final String DISPATCH_QUEUE_NAME = "dispatch";
    public static final String DISPATCH_DLQ_NAME = "dispatch.dlq";
    public static final String DISTRIBUTION_DLX = "distribution.dlx";
    public static final String DLQ_REASON = "x-first-death-reason";
    public static final String DLQ_ORIGINAL_ROUTING_KEY = "x-death-original-routing-key";
    public static final String DISPATCHER_CONNECTION_NAME = "dispatcher";

    private final CachingConnectionFactory connectionFactory;

    private RabbitProperties rabbitProperties;

    @Autowired
    public void RabbitMQConfig(RabbitProperties rabbitProperties) {
        this.rabbitProperties = rabbitProperties;
    }

    public AmqpConfiguration(CachingConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
        // https://stackoverflow.com/questions/49089915/how-to-set-custom-name-for-rabbitmq-connection
        this.connectionFactory.setConnectionNameStrategy(f -> DISPATCHER_CONNECTION_NAME);
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
