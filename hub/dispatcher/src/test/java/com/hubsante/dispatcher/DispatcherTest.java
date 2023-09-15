package com.hubsante.dispatcher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hubsante.hub.HubApplication;
import com.hubsante.hub.config.HubClientConfiguration;
import com.hubsante.hub.service.Dispatcher;
import com.hubsante.hub.service.EdxlHandler;
import com.hubsante.hub.service.ContentMessageHandler;
import com.hubsante.hub.service.Validator;
import com.hubsante.model.CustomMessage;
import com.hubsante.model.edxl.EdxlMessage;
import com.hubsante.model.report.ErrorCode;
import com.hubsante.model.report.ErrorReport;
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
import java.util.Arrays;
import java.util.Objects;

import static com.hubsante.dispatcher.utils.MessageTestUtils.*;
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
    @Autowired
    private Validator validator;
    static ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    private Dispatcher dispatcher;
    private final String SAMU_B_ROUTING_KEY = "fr.health.samuB";
    private final String SAMU_B_INFO_QUEUE = SAMU_B_ROUTING_KEY + ".info";
    private final String SAMU_A_ROUTING_KEY = "fr.health.samuA";
    private final String SAMU_A_INFO_QUEUE = SAMU_A_ROUTING_KEY + ".info";
    private final String INCONSISTENT_ROUTING_KEY = "fr.health.no-samu";

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry propertiesRegistry) {
        propertiesRegistry.add("client.preferences.file",
                () -> Objects.requireNonNull(classLoader.getResource("config/client.preferences.csv")));
        propertiesRegistry.add("hubsante.default.message.ttl", () -> 5);
    }

    @PostConstruct
    public void init() {
        dispatcher = new Dispatcher(rabbitTemplate, converter, hubConfig, validator);
    }

    @Test
    @DisplayName("should send xml message to the right exchange and routing key")
    public void shouldDispatchXmlToRightExchange() throws IOException {
        Message receivedMessage = createMessage("valid/edxl_encapsulated/samuB_to_nexsis.xml", SAMU_B_ROUTING_KEY);
        dispatcher.dispatch(receivedMessage);

        // assert that the message was sent to the right exchange with the right routing key exactly 1 time
        Mockito.verify(rabbitTemplate, times(1)).send(
                eq(DISTRIBUTION_EXCHANGE), eq("fr.fire.nexsis.sdisZ.message"), any(Message.class));

    }

    @Test
    @DisplayName("should convert message from xml to json according to client preferences")
    public void shouldConvertMessageFromXmlToJson() throws IOException {
        Message receivedMessage = createMessage("valid/edxl_encapsulated/samuB_to_samuA.xml", SAMU_B_ROUTING_KEY);
        assertEquals(MessageProperties.CONTENT_TYPE_XML, receivedMessage.getMessageProperties().getContentType());
        dispatcher.dispatch(receivedMessage);

        // assert that the message was sent to the right exchange with the right routing key exactly 1 time
        ArgumentCaptor<Message> argCaptor = ArgumentCaptor.forClass(Message.class);
        Mockito.verify(rabbitTemplate, times(1)).send(
                eq(DISTRIBUTION_EXCHANGE), eq("fr.health.samuA.message"), argCaptor.capture());
        Message sentMessage = argCaptor.getValue();
        assertEquals(MessageProperties.CONTENT_TYPE_JSON, sentMessage.getMessageProperties().getContentType());

        EdxlMessage publishedEDXLMessage = converter.deserializeXmlEDXL(new String(receivedMessage.getBody(), StandardCharsets.UTF_8));
        EdxlMessage sentEDXLMessage = converter.deserializeJsonEDXL(new String(sentMessage.getBody(), StandardCharsets.UTF_8));
        assertEquals(publishedEDXLMessage, sentEDXLMessage);
    }

    @Test
    @DisplayName("should convert message from json to xml according to client preferences")
    public void shouldConvertMessageFromJsonToXml() throws IOException {
        Message receivedMessage = createMessage("valid/edxl_encapsulated/samuA_to_samuB.json", SAMU_A_ROUTING_KEY);
        assertEquals(MessageProperties.CONTENT_TYPE_JSON, receivedMessage.getMessageProperties().getContentType());
        dispatcher.dispatch(receivedMessage);

        // assert that the message was sent to the right exchange with the right routing key exactly 1 time
        ArgumentCaptor<Message> argCaptor = ArgumentCaptor.forClass(Message.class);
        Mockito.verify(rabbitTemplate, times(1)).send(
                eq(DISTRIBUTION_EXCHANGE), eq("fr.health.samuB.message"), argCaptor.capture());
        Message sentMessage = argCaptor.getValue();
        assertEquals(MessageProperties.CONTENT_TYPE_XML, sentMessage.getMessageProperties().getContentType());

        EdxlMessage jsonEDXLMessage = converter.deserializeJsonEDXL(new String(receivedMessage.getBody(), StandardCharsets.UTF_8));
        EdxlMessage xmlEDXLMessage = converter.deserializeXmlEDXL(new String(sentMessage.getBody(), StandardCharsets.UTF_8));
        assertEquals(jsonEDXLMessage, xmlEDXLMessage);
    }

    @Test
    @DisplayName("should send Json message to the right exchange and routing key")
    public void shouldDispatchJsonToRightExchange() throws IOException {
        Message receivedMessage = createMessage("valid/edxl_encapsulated/samuA_to_nexsis.json", SAMU_A_ROUTING_KEY);
        dispatcher.dispatch(receivedMessage);

        // assert that the message was sent to the right exchange with the right routing key exactly 1 time
        Mockito.verify(rabbitTemplate, times(1)).send(
                eq(DISTRIBUTION_EXCHANGE), eq("fr.fire.nexsis.sdisZ.message"), any(Message.class));
    }

    @Test
    @DisplayName("custom message should be dispatched to the right exchange")
    public void shouldDispatchCustomMessageToRightExchange() throws IOException {
        Message receivedMessage = createMessage("valid/edxl_encapsulated/genericMessage.json", SAMU_B_ROUTING_KEY);
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
        Message base = createMessage("valid/edxl_encapsulated/samuB_to_nexsis.xml", MessageProperties.CONTENT_TYPE_XML, SAMU_B_ROUTING_KEY);
        EdxlMessage edxlMessage = converter.deserializeXmlEDXL(new String(base.getBody(), StandardCharsets.UTF_8));
        OffsetDateTime now = OffsetDateTime.now();
        edxlMessage.setDateTimeSent(now);
        edxlMessage.setDateTimeExpires(now.plusSeconds(2));
        Message customTTLMessage = new Message(converter.serializeXmlEDXL(edxlMessage).getBytes(), base.getMessageProperties());

        // before dispatch, the message has no expiration set
        assertNull(customTTLMessage.getMessageProperties().getExpiration());
        // method call
        dispatcher.dispatch(customTTLMessage);
        // we capture the forwarded message to ensure that it has been overwritten
        ArgumentCaptor<Message> argument = ArgumentCaptor.forClass(Message.class);
        Mockito.verify(rabbitTemplate, times(1)).send(
                eq(DISTRIBUTION_EXCHANGE), eq("fr.fire.nexsis.sdisZ.message"), argument.capture());

        // when calling rabbitTemplate.send(), the message has new expiration set
        assertNotNull(argument.getValue().getMessageProperties().getExpiration());
    }

    @Test
    @DisplayName("should send info to sender of DLQed message - expiration")
    public void handleDLQMessage() throws Exception {
        // we test that the message has been rejected after the DLQ listener has been called
        Message originalMessage = createMessage("valid/edxl_encapsulated/samuB_to_nexsis.xml", MessageProperties.CONTENT_TYPE_XML, SAMU_B_ROUTING_KEY);
        Message dlqMessage = applyRabbitmqDLQHeaders(originalMessage, "expired");
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> dispatcher.dispatchDLQ(dlqMessage));

        // we test that an error report has been sent with the correct error code
        assertErrorReportHasBeenSent(
                SAMU_B_INFO_QUEUE, ErrorCode.DEAD_LETTER_QUEUED,
                "samuB_2608323d-507d-4cbf-bf74-52007f8124ea",
                "has been read from dead-letter-queue; reason was expired");
    }

    @Test
    @DisplayName("malformed message should throw an exception")
    public void malformedMessagefailed() throws IOException {

        // we test that the message has been rejected if we can't parse it
        Message receivedMessage = createMessage("serialization/edxlWithMalformedContent.json", SAMU_B_ROUTING_KEY);
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> dispatcher.dispatch(receivedMessage));

        assertErrorReportHasBeenSent(SAMU_B_INFO_QUEUE, ErrorCode.UNRECOGNIZED_MESSAGE_FORMAT,
                "Could not parse message, invalid format. \n If you don't want to use HubSanté model" +
                        " for now, please use a \"customContent\" wrapper inside your message.");
    }

    @Test
    @DisplayName("message without content-type is rejected")
    public void rejectMessageWithoutContentType() throws IOException {
        // we test that the message has been rejected if the content-type is not set
        Message receivedMessage = createMessage("valid/edxl_encapsulated/samuA_to_nexsis.json", null, SAMU_B_ROUTING_KEY);
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> dispatcher.dispatch(receivedMessage));

        // we test that an error report has been sent with the correct error code
        assertErrorReportHasBeenSent(SAMU_B_INFO_QUEUE, ErrorCode.NOT_ALLOWED_CONTENT_TYPE,
                "Unhandled Content-Type ! Message Content-Type should be set at 'application/json' or 'application/xml'");
    }

    @Test
    @DisplayName("message with unhandled content-type is rejected")
    public void rejectMessageWithUnhandledContentType() throws IOException {
        // we test that the message has been rejected if the content-type is neither json nor xml
        Message receivedMessage = createMessage("valid/edxl_encapsulated/samuA_to_nexsis.json", MessageProperties.DEFAULT_CONTENT_TYPE, SAMU_A_ROUTING_KEY);
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> dispatcher.dispatch(receivedMessage));

        // we test that an error report has been sent with the correct error code
        assertErrorReportHasBeenSent(SAMU_A_INFO_QUEUE, ErrorCode.NOT_ALLOWED_CONTENT_TYPE,
                "Unhandled Content-Type ! Message Content-Type should be set at 'application/json' or 'application/xml'");
    }

    @Test
    @DisplayName("message body inconsistent with content-type is rejected")
    public void rejectMessageWithInconsistentBody() throws IOException {
        // we test that the message has been rejected if the body is not consistent with the content-type
        Message receivedMessage = createMessage("valid/edxl_encapsulated/samuA_to_nexsis.json", MessageProperties.CONTENT_TYPE_XML, SAMU_A_ROUTING_KEY);
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> dispatcher.dispatch(receivedMessage));

        // we test that an error report has been sent with the correct error code
        assertErrorReportHasBeenSent(SAMU_A_INFO_QUEUE, ErrorCode.UNRECOGNIZED_MESSAGE_FORMAT,
                "Could not parse message, invalid format. \n If you don't want to use HubSanté model" +
                        " for now, please use a \"customContent\" wrapper inside your message.");
    }

    @Test
    @DisplayName("outer routing key inconsistent with sender ID")
    public void outerRoutingKeyInconsistentWithSenderId() throws IOException {
        // we test that the message has been rejected if the sender ID is not consistent with the outer routing key
        Message receivedMessage = createMessage("valid/edxl_encapsulated/samuA_to_nexsis.json", INCONSISTENT_ROUTING_KEY);
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> dispatcher.dispatch(receivedMessage));

        // we test that an error report has been sent with the correct error code
        assertErrorReportHasBeenSent(INCONSISTENT_ROUTING_KEY + ".info", ErrorCode.SENDER_INCONSISTENCY,
                "message sender is fr.health.samuA", "received routing key is fr.health.no-samu");
    }

    @Test
    @DisplayName("should reject message without persistent delivery mode")
    public void rejectMessageWithoutPersistentDeliveryMode() throws IOException {
        Message receivedMessage = createMessage("valid/edxl_encapsulated/samuB_to_nexsis.xml", MessageProperties.CONTENT_TYPE_XML, SAMU_B_ROUTING_KEY);
        receivedMessage.getMessageProperties().setReceivedDeliveryMode(MessageDeliveryMode.NON_PERSISTENT);
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> dispatcher.dispatch(receivedMessage));

        // we test that an error report has been sent with the correct error code
        assertErrorReportHasBeenSent(SAMU_B_INFO_QUEUE, ErrorCode.DELIVERY_MODE_INCONSISTENCY,
                "samuB_2608323d-507d-4cbf-bf74-52007f8124ea", "non-persistent delivery mode");
    }

    @Test
    @DisplayName("should reject message with invalid json EDXL envelope")
    public void invalidJsonEDXLFails() throws IOException {
        Message receivedMessage = createMessage("invalid/missingEDXLRequiredValues.json",
                MessageProperties.CONTENT_TYPE_JSON, SAMU_A_ROUTING_KEY);
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> dispatcher.dispatch(receivedMessage));

        assertErrorReportHasBeenSent(SAMU_A_INFO_QUEUE, ErrorCode.INVALID_MESSAGE,
                "$.distributionID est un champ obligatoire mais manquant",
                "$.descriptor.explicitAddress.explicitAddressValue est un champ obligatoire mais manquant");
    }

    @Test
    @DisplayName("should reject message with invalid json content")
    public void invalidJsonContentFails() throws IOException {
        Message receivedMessage = createMessage("invalid/invalidCreateMessageValidEdxlEnvelope.json",
                MessageProperties.CONTENT_TYPE_JSON, SAMU_A_ROUTING_KEY);
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> dispatcher.dispatch(receivedMessage));

        assertErrorReportHasBeenSent(SAMU_A_INFO_QUEUE, ErrorCode.INVALID_MESSAGE,
                "$.createdAt est un champ obligatoire mais manquant");
    }

    private void assertErrorReportHasBeenSent(String infoQueueName, ErrorCode errorCode, String... errorCause) throws JsonProcessingException {

        ArgumentCaptor<Message> argument = ArgumentCaptor.forClass(Message.class);
        Mockito.verify(rabbitTemplate, times(1)).send(
                eq(DISTRIBUTION_EXCHANGE), eq(infoQueueName), argument.capture());

        ErrorReport errorReport = getErrorReportFromMessage(converter, argument);
        assertEquals(errorCode, errorReport.getErrorCode());
        if (errorCause != null) {
            Arrays.stream(errorCause).forEach(cause -> assertTrue(errorReport.getErrorCause().contains(cause)));
        }
    }
}
