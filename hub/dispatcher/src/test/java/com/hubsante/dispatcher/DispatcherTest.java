package com.hubsante.dispatcher;

import com.hubsante.hub.HubApplication;
import com.hubsante.hub.config.HubClientConfiguration;
import com.hubsante.hub.service.Dispatcher;
import com.hubsante.hub.service.EdxlHandler;
import com.hubsante.model.CustomMessage;
import com.hubsante.model.edxl.EdxlMessage;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.test.context.SpringRabbitTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Objects;

import static com.hubsante.dispatcher.utils.MessageTestUtils.createMessage;
import static com.hubsante.hub.config.AmqpConfiguration.*;
import static org.junit.jupiter.api.Assertions.*;
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
    private final String XML_MESSAGE_ROUTING_KEY = "fr.health.samu069";
    private final String JSON_MESSAGE_ROUTING_KEY = "fr.health.samu069";

    private final String INCONSISTENT_ROUTING_KEY = "fr.health.no-samu";

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry propertiesRegistry) {
        propertiesRegistry.add("client.preferences.file",
                () -> Objects.requireNonNull(classLoader.getResource("config/client.preferences.csv")));
        propertiesRegistry.add("hubsante.default.message.ttl", () -> 5);
    }

    @PostConstruct
    public void init() {
        dispatcher = new Dispatcher(rabbitTemplate, converter, hubConfig);
    }

    @Test
    @DisplayName("should send message to the right exchange and routing key")
    public void shouldDispatchToRightExchange() throws IOException {
        Message receivedMessage = createMessage("createCaseEdxl.xml", MessageProperties.CONTENT_TYPE_XML, XML_MESSAGE_ROUTING_KEY);
        dispatcher.dispatch(receivedMessage);

        // assert that the message was sent to the right exchange with the right routing key exactly 1 time
        Mockito.verify(rabbitTemplate, times(1)).send(
                eq(DISTRIBUTION_EXCHANGE), eq("fr.fire.nexsis.sdis23.message"), any(Message.class));
    }

    @Test
    @DisplayName("custom message should be dispatched to the right exchange")
    public void shouldDispatchCustomMessageToRightExchange() throws IOException {
        Message receivedMessage = createMessage("genericMessage.json", JSON_MESSAGE_ROUTING_KEY);
        assert(receivedMessage.getMessageProperties().getContentType().equals(MessageProperties.CONTENT_TYPE_JSON));
        dispatcher.dispatch(receivedMessage);

        ArgumentCaptor<Message> argument = ArgumentCaptor.forClass(Message.class);
        Mockito.verify(rabbitTemplate, times(1)).send(
                eq(DISTRIBUTION_EXCHANGE), eq("fr.health.samu70.message"), argument.capture());

        EdxlMessage edxlMessage = converter.deserializeJsonEDXL(
                new String(argument.getValue().getBody()));
        CustomMessage customMessage = edxlMessage.getContent().getContentObject()
                .getContentWrapper().getEmbeddedContent().getMessage();

        assertEquals("value1", customMessage.getCustomContent().get("prop1").asText());
    }

    @Test
    @DisplayName("should reset TTL if edxl dateTimeExpires is lower")
    public void shouldResetTTL() throws IOException {
        // get message and override dateTimeExpires field with sooner value
        Message base = createMessage("createCaseEdxl.xml", MessageProperties.CONTENT_TYPE_XML, XML_MESSAGE_ROUTING_KEY);
        EdxlMessage edxlMessage = converter.deserializeXmlEDXL(new String(base.getBody(), StandardCharsets.UTF_8));
        edxlMessage.setDateTimeExpires(OffsetDateTime.now().plusSeconds(1));
        Message customTTLMessage = new Message(converter.serializeXmlEDXL(edxlMessage).getBytes(), base.getMessageProperties());

        // before dispatch, the message has no expiration set
        assertNull(customTTLMessage.getMessageProperties().getExpiration());
        // method call
        dispatcher.dispatch(customTTLMessage);
        // we capture the forwarded message to ensure that it has been overwritten
        ArgumentCaptor<Message> argument = ArgumentCaptor.forClass(Message.class);
        Mockito.verify(rabbitTemplate, times(1)).send(
                eq(DISTRIBUTION_EXCHANGE), eq("fr.fire.nexsis.sdis23.message"), argument.capture());

        // when calling rabbitTemplate.send(), the message has new expiration set
        assertNotNull(customTTLMessage.getMessageProperties().getExpiration());
        assertEquals(
                argument.getValue().getMessageProperties().getExpiration(),
                customTTLMessage.getMessageProperties().getExpiration());
    }

    @Test
    @DisplayName("should send info to sender of DLQed message - expiration")
    public void handleDLQMessage() throws Exception {
        Message receivedMessage = createMessage("createCaseEdxl.xml", MessageProperties.CONTENT_TYPE_XML, XML_MESSAGE_ROUTING_KEY);
        receivedMessage.getMessageProperties().setHeader(DLQ_REASON, "expired");
        receivedMessage.getMessageProperties().setHeader(DLQ_MESSAGE_ORIGIN, "fr.fire.nexsis.sdis23.message");
        dispatcher.dispatchDLQ(receivedMessage);

        ArgumentCaptor<Message> argument = ArgumentCaptor.forClass(Message.class);
        Mockito.verify(rabbitTemplate, times(1)).send(
                eq(DISTRIBUTION_EXCHANGE), eq("fr.health.samu069.info"), argument.capture());
        String str = new String(argument.getValue().getBody());
        assert(str.endsWith("has not been consumed on fr.fire.nexsis.sdis23.message"));
    }

    @Test
    @DisplayName("malformed message should throw an exception")
    public void malformedMessagefailed() throws IOException {
        //TODO bbo : without validation, only type errors are detected.
        //missing required fields are not
        // only validation will do
        Message receivedMessage = createMessage("edxlWithMalformedContent.json", JSON_MESSAGE_ROUTING_KEY);
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> dispatcher.dispatch(receivedMessage));
        ArgumentCaptor<Message> argument = ArgumentCaptor.forClass(Message.class);
        Mockito.verify(rabbitTemplate, times(1)).send(
                eq(DISTRIBUTION_EXCHANGE), eq("fr.health.samu069.info"), argument.capture());
        assert(new String(argument.getValue().getBody()).startsWith("Could not parse message, invalid format"));
    }

    @Test
    @DisplayName("message without content-type is rejected")
    public void rejectMessageWithoutContentType() throws IOException {
        Message receivedMessage = createMessage("createCaseEdxl.json", null, JSON_MESSAGE_ROUTING_KEY);
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> dispatcher.dispatch(receivedMessage));
        ArgumentCaptor<Message> argument = ArgumentCaptor.forClass(Message.class);
        Mockito.verify(rabbitTemplate, times(1)).send(
                eq(DISTRIBUTION_EXCHANGE), eq("fr.health.samu069.info"), argument.capture());
        assertEquals("Unhandled Content-Type ! Message Content-Type should be set at 'application/json' or 'application/xml'",
                new String(argument.getValue().getBody()));
    }

    @Test
    @DisplayName("message with unhandled content-type is rejected")
    public void rejectMessageWithUnhandledContentType() throws IOException {
        Message receivedMessage = createMessage("createCaseEdxl.json", MessageProperties.DEFAULT_CONTENT_TYPE, JSON_MESSAGE_ROUTING_KEY);
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> dispatcher.dispatch(receivedMessage));
    }

    @Test
    @DisplayName("message body inconsistent with content-type is rejected")
    public void rejectMessageWithInconsistentBody() throws IOException {
        Message receivedMessage = createMessage("createCaseEdxl.json", MessageProperties.CONTENT_TYPE_XML, JSON_MESSAGE_ROUTING_KEY);
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> dispatcher.dispatch(receivedMessage));
    }

    @Test
    @DisplayName("outer routing key inconsistent with sender ID")
    public void outerRoutingKeyInconsistentWithSenderId() throws IOException {
        Message receivedMessage = createMessage("createCaseEdxl.json", INCONSISTENT_ROUTING_KEY);
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> dispatcher.dispatch(receivedMessage));
        ArgumentCaptor<Message> argument = ArgumentCaptor.forClass(Message.class);
        Mockito.verify(rabbitTemplate, times(1)).send(
                eq(DISTRIBUTION_EXCHANGE), eq("fr.health.samu069.info"), argument.capture());
        assert(new String(argument.getValue().getBody()).startsWith("Sender inconsistency for message"));
    }

    @Test
    @DisplayName("should forward message with persistent delivery mode")
    public void forwardMessageWithPersistentDeliveryMode() throws IOException {
        Message receivedMessage = createMessage("createCaseEdxl.xml", MessageProperties.CONTENT_TYPE_XML, XML_MESSAGE_ROUTING_KEY);
        receivedMessage.getMessageProperties().setDeliveryMode(MessageDeliveryMode.NON_PERSISTENT);
        dispatcher.dispatch(receivedMessage);

        ArgumentCaptor<Message> sentMessage = ArgumentCaptor.forClass(Message.class);
        Mockito.verify(rabbitTemplate, times(1)).send(
                eq(DISTRIBUTION_EXCHANGE), eq("fr.fire.nexsis.sdis23.message"), sentMessage.capture());

        assertEquals(MessageDeliveryMode.PERSISTENT, sentMessage.getValue().getMessageProperties().getDeliveryMode());
    }
}
