package com.hubsante.hub.service;

import java.util.Map;
import com.rabbitmq.client.Channel;

import org.springframework.stereotype.Component;
import org.springframework.amqp.rabbit.core.ChannelCallback;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

@Component
public class RabbitHealthIndicator implements HealthIndicator {
    private final RabbitTemplate rabbitTemplate;
    @Value("${spring.rabbitmq.host}")
    private String rabbitHostname;
    @Value("${spring.rabbitmq.virtual-host}")
    private String rabbitVirtualHost;

    public RabbitHealthIndicator(RabbitTemplate rabbitTemplate) {
		this.rabbitTemplate = rabbitTemplate;
	}
    
    public final Health health() {
        Health.Builder builder = new Health.Builder();
        try {
            String version = this.rabbitTemplate.execute(new ChannelCallback<String>() {
                @Override
                public String doInRabbit(Channel channel) throws Exception {
                    Map<String, Object> serverProperties = channel.getConnection()
                            .getServerProperties();
                    return serverProperties.get("version").toString();
                }
            });
            
            builder.up().withDetails(Map.of(
                "version",version,
                "host",rabbitHostname,
                "vhost",rabbitVirtualHost
            ));
        } catch(Exception exception) {
            builder.down(exception);
        }

        return builder.build();
    }
}
