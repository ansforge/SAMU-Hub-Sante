package com.hubsante.hub.service;

import org.junit.jupiter.api.*;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.containers.Container;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Objects;

import static com.hubsante.hub.service.utils.MessageTestUtils.createInvalidMessage;
import static com.hubsante.hub.service.utils.MessageTestUtils.createMessage;
import static org.junit.jupiter.api.Assertions.*;

public class RabbitBatchTest extends RabbitIntegrationAbstract {

    @Autowired
    private AmqpAdmin amqpAdmin;
    private static final int MAX_DELIVERY_RATE = 120;

    @BeforeEach
    public void setUp() throws IOException, InterruptedException {
        // update TTL policy because default test TTL is very low (5s) and inappropriate for batch purposes
        rabbitMQContainer.execInContainer(
                "rabbitmqctl", "set_policy", "dead-lettering",
                "^.*\\.(?:ack|message)$",
                "{\"message-ttl\": 300000,\"dead-letter-exchange\": \"distribution.dlx\"}",
                "-p", "/",
                "--priority", "0",
                "--apply-to", "queues"
        );
        rabbitMQContainer.execInContainer(
                "rabbitmqctl", "set_policy", "dead-lettering-info",
                "^.*\\.info",
                "{\"message-ttl\": 300000,\"dead-letter-exchange\": \"distribution.dlx\"}",
                "-p", "/",
                "--priority", "0",
                "--apply-to", "queues"
        );
    }

    @Disabled
    @Test
    @DisplayName("publish and consume batch of 1000 messages")
    public void consumeBatch() throws IOException, InterruptedException {
        assertEquals(0, Objects.requireNonNull(amqpAdmin.getQueueInfo(SAMU_B_MESSAGE_QUEUE)).getMessageCount());
        assertEquals(0, Objects.requireNonNull(amqpAdmin.getQueueInfo(SAMU_A_INFO_QUEUE)).getMessageCount());

        OffsetDateTime batchStartTime = OffsetDateTime.now();
        Container.ExecResult batchOutput = publishBatch(1000);
        OffsetDateTime batchEndTime = OffsetDateTime.now();

        int published = Objects.requireNonNull(amqpAdmin.getQueueInfo(SAMU_B_MESSAGE_QUEUE)).getMessageCount();
        int rejected = Objects.requireNonNull(amqpAdmin.getQueueInfo(SAMU_A_INFO_QUEUE)).getMessageCount();

        assertEquals(900, published);
        assertEquals(100, rejected);

        long duration = batchEndTime.toInstant().toEpochMilli() - batchStartTime.toInstant().toEpochMilli();
        long averageDeliveryRate = duration / (published + rejected);
        // assert average delivery rate is lower than a determined threshold in milliseconds
        // This rate contains both publishing rate & Dispatcher forwarding
        assertTrue(averageDeliveryRate < MAX_DELIVERY_RATE);
    }

    private Container.ExecResult publishBatch(int batchSize) throws IOException, InterruptedException {
        String valid = new String(createMessage("EDXL-DE", JSON, SAMU_A_ROUTING_KEY).getBody());
        String invalid = new String(
                createInvalidMessage("EDXL-DE/missing-EDXL-required-field.json", SAMU_A_ROUTING_KEY).getBody());

        // exec mounted batch publish bash script
        return rabbitMQContainer.execInContainer(
                "bash", "/tmp/rabbitmq/config/batch-test.sh", String.valueOf(batchSize), valid, invalid, SAMU_A_ROUTING_KEY
        );
    }
}
