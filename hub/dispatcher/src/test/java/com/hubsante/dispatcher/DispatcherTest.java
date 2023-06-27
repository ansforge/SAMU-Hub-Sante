package com.hubsante.dispatcher;

import com.hubsante.hub.HubApplication;
import com.hubsante.hub.config.HubClientConfiguration;
import com.hubsante.hub.service.Dispatcher;
import com.hubsante.hub.service.EdxlHandler;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePropertiesBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.test.context.SpringRabbitTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

@SpringBootTest
@ContextConfiguration(classes = HubApplication.class)
@SpringRabbitTest
@Slf4j
public class DispatcherTest {

    private RabbitTemplate rabbitTemplate = Mockito.mock(RabbitTemplate.class);

    @Autowired
    private EdxlHandler converter;
    @Autowired
    private HubClientConfiguration hubConfig;
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry propertiesRegistry) {
        propertiesRegistry.add("client.preferences.file",
                () -> "file:C:/dev/ANS/SAMU/HubSante/repository/SAMU-Hub-Sante/hub/dispatcher/src/test/resources/config/client.preferences.csv");
    }


    @Test
    @DisplayName("should send message to the right exchange and routing key")
    public void shouldDispatchToRightExchange() throws IOException {
        File edxlCisuCreateFile = new File(classLoader.getResource("cisuCreateEdxl.xml").getFile());
        String xml = Files.readString(edxlCisuCreateFile.toPath());

        MessageProperties properties = MessagePropertiesBuilder.newInstance()
                .setReceivedRoutingKey("fr.health.hub.samu110.out.message")
                .setContentType("application/xml")
                .build();
        Message receivedMessage = new Message(xml.getBytes(StandardCharsets.UTF_8), properties);

        Dispatcher dispatcher = new Dispatcher(rabbitTemplate, converter, hubConfig);
        dispatcher.dispatch(receivedMessage);

        // assert that the message was sent to the right exchange with the right routing key exactly 1 time
        Mockito.verify(rabbitTemplate, times(1)).send(
                eq(""), eq("fr.fire.nexsis.sdis23.in.message"), any());
    }

    @Test
    @DisplayName("malformed message should throw an exception")
    public void malformedMessagefailed() throws IOException {
        File malformedEdxlFile = new File(classLoader.getResource("malformedEdxl.json").getFile());
        String json = Files.readString(malformedEdxlFile.toPath());

        MessageProperties properties = MessagePropertiesBuilder.newInstance()
                .setReceivedRoutingKey("fr.health.hub.samu050.out.message")
                .setContentType("application/json").build();
        Message receivedMessage = new Message(json.getBytes(StandardCharsets.UTF_8), properties);

        Dispatcher dispatcher = new Dispatcher(rabbitTemplate, converter, hubConfig);

        assertThrows(AmqpRejectAndDontRequeueException.class, () -> {
            dispatcher.dispatch(receivedMessage);
        });
    }
}
