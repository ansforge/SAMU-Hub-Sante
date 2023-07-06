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

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Objects;

import static com.hubsante.dispatcher.utils.MessageTestUtils.createMessage;
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
    static ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    private Dispatcher dispatcher;
    private final String XML_MESSAGE_ROUTING_KEY = "fr.health.samu110.out.message";
    private final String JSON_MESSAGE_ROUTING_KEY = "fr.health.samu050.out.message";

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry propertiesRegistry) {
        propertiesRegistry.add("client.preferences.file",
                () -> Objects.requireNonNull(classLoader.getResource("config/client.preferences.csv")));
    }

    @PostConstruct
    public void init() {
        dispatcher = new Dispatcher(rabbitTemplate, converter, hubConfig);
    }


    @Test
    @DisplayName("should send message to the right exchange and routing key")
    public void shouldDispatchToRightExchange() throws IOException {
        Message receivedMessage = createMessage("cisuCreateEdxl.xml", MessageProperties.CONTENT_TYPE_XML, XML_MESSAGE_ROUTING_KEY);
        dispatcher.dispatch(receivedMessage);

        // assert that the message was sent to the right exchange with the right routing key exactly 1 time
        Mockito.verify(rabbitTemplate, times(1)).send(
                eq(""), eq("fr.fire.nexsis.sdis23.in.message"), any());
    }

    @Test
    @DisplayName("malformed message should throw an exception")
    public void malformedMessagefailed() throws IOException {
        Message receivedMessage = createMessage("edxlWithMalformedContent.json", JSON_MESSAGE_ROUTING_KEY);
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> {
            dispatcher.dispatch(receivedMessage);
        });
    }

    @Test
    @DisplayName("message without content-type is rejected")
    public void rejectMessageWithoutContentType() throws IOException {
        Message receivedMessage = createMessage("cisuCreateEdxl.json", null, JSON_MESSAGE_ROUTING_KEY);
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> {
            dispatcher.dispatch(receivedMessage);
        });
    }

    @Test
    @DisplayName("message with unhandled content-type is rejected")
    public void rejectMessageWithUnhandledContentType() throws IOException {
        Message receivedMessage = createMessage("cisuCreateEdxl.json", MessageProperties.DEFAULT_CONTENT_TYPE, JSON_MESSAGE_ROUTING_KEY);
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> {
            dispatcher.dispatch(receivedMessage);
        });
    }

    @Test
    @DisplayName("message body inconsistent with content-type is rejected")
    public void rejectMessageWithInconsistentBody() throws IOException {
        Message receivedMessage = createMessage("cisuCreateEdxl.json", MessageProperties.CONTENT_TYPE_XML, JSON_MESSAGE_ROUTING_KEY);
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> {
            dispatcher.dispatch(receivedMessage);
        });
    }

    @Test
    @DisplayName("outer routing key inconsistent with sender ID")
    public void outerRoutingKeyInconsistentWithSenderId() throws IOException {
        Message receivedMessage = createMessage("cisuCreateEdxl.json", XML_MESSAGE_ROUTING_KEY);
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> {
            dispatcher.dispatch(receivedMessage);
        });
    }
}
