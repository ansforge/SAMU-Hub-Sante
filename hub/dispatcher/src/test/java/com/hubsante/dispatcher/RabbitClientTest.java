package com.hubsante.dispatcher;

import com.hubsante.hub.HubApplication;
import com.hubsante.hub.service.Dispatcher;
import com.hubsante.hub.service.EdxlHandler;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.MessageProperties;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;


import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;

import static com.hubsante.hub.config.AmqpConfiguration.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;

@SpringBootTest
@ContextConfiguration(classes = HubApplication.class
//        , initializers = RabbitClientTest.Initializer.class
)
@ActiveProfiles("test")
@Testcontainers
@Slf4j
public class RabbitClientTest {
    private static final String HUBSANTE_EXCHANGE = "hubsante";

    private static final String RABBITMQ_IMAGE = "rabbitmq:3.11-management-alpine";
    private static final String ENTRY_QUEUE = "*.out.*";
    private static final String SENDER_ACK_QUEUE = "fr.health.hub.samu110.out.ack";
    private static final String SENDER_MESSAGE_ROUTING_KEY = "fr.health.hub.samu110.out.message";
    private static final String SENDER_ACK_ROUTING_KEY = "fr.health.hub.samu110.out.ack";
    private static final String RECIPIENT_QUEUE = "fr.fire.nexsis.sdis23.in.message";
    private static final String RECIPIENT_ROUTING_KEY = "fr.fire.nexsis.sdis23.in.message";


//    @Autowired
//    private Dispatcher dispatcher;

//    @Autowired
    private RabbitTemplate rabbitTemplate = Mockito.mock(RabbitTemplate.class);

    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    @Autowired
    private EdxlHandler converter;

    @Test
    public void test() {
        Dispatcher dispatcher = new Dispatcher(rabbitTemplate, converter);
        MessageProperties properties;
        Message message = new Message("test".getBytes(StandardCharsets.UTF_8));

        dispatcher.dispatch(message);
        Mockito.verify(rabbitTemplate, times(1)).send("", "", message);
    }

//    @Container
//    public static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer(
//            DockerImageName.parse(RABBITMQ_IMAGE))
//            .withPluginsEnabled("rabbitmq_management")
//            .withExchange(HUBSANTE_EXCHANGE, "topic")
//
//            .withQueue(ENTRY_QUEUE)
//            .withBinding(HUBSANTE_EXCHANGE, ENTRY_QUEUE, Collections.emptyMap(), MESSAGE_ROUTING_KEY, "queue")
//            .withBinding(HUBSANTE_EXCHANGE, ENTRY_QUEUE, Collections.emptyMap(), ACK_ROUTING_KEY, "queue")
//
//            .withQueue(SENDER_ACK_QUEUE)
//            .withBinding(HUBSANTE_EXCHANGE, SENDER_ACK_QUEUE, Collections.emptyMap(), SENDER_ACK_ROUTING_KEY, "queue")
//
//            .withQueue(RECIPIENT_QUEUE)
//            .withBinding(HUBSANTE_EXCHANGE, RECIPIENT_QUEUE, Collections.emptyMap(), RECIPIENT_ROUTING_KEY, "queue");

//    @Test
//    public void dispatchTest() throws IOException, InterruptedException {
//        rabbitMQContainer.start();
//        Integer port = rabbitMQContainer.getMappedPort(15672);
//
//        File edxlCisuCreateFile = new File(classLoader.getResource("cisuCreateEdxl.xml").getFile());
//        String json = Files.readString(edxlCisuCreateFile.toPath());
//
//        Message published = new Message(json.getBytes(StandardCharsets.UTF_8));
//        rabbitTemplate.sendAndReceive(HUBSANTE_EXCHANGE, SENDER_MESSAGE_ROUTING_KEY, published);
//        Thread.sleep(5000);
//
//        Message received = rabbitTemplate.receive(RECIPIENT_QUEUE);
//        Assertions.assertEquals(json, new String(received.getBody(), StandardCharsets.UTF_8));
//        System.out.println("breakpoint");
//    }

//    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
//
//        @Override
//        public void initialize(ConfigurableApplicationContext applicationContext) {
//            val values = TestPropertyValues.of(
//                    "spring.rabbitmq.host=" + rabbitMQContainer.getHost(),
//                    "spring.rabbitmq.port=" + rabbitMQContainer.getMappedPort(5672)
//            );
//            values.applyTo(applicationContext);
//        }
//    }
}
