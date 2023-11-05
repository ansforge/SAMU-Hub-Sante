package com.hubsante.hub.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hubsante.hub.HubApplication;
import com.hubsante.hub.config.HubConfiguration;
import com.hubsante.model.EdxlHandler;
import com.hubsante.model.Validator;
import com.hubsante.model.custom.CustomMessage;
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
import java.util.Arrays;
import java.util.Objects;

import static com.hubsante.hub.config.AmqpConfiguration.*;
import static com.hubsante.hub.service.utils.MessageTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@ContextConfiguration(classes = HubApplication.class)
@SpringRabbitTest
@Slf4j
public class DispatcherTest {

    private RabbitTemplate rabbitTemplate = Mockito.mock(RabbitTemplate.class);

    @Autowired
    private EdxlHandler converter;
    @Autowired
    private HubConfiguration hubConfig;
    @Autowired
    private Validator validator;
    static ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    private Dispatcher dispatcher;
    private final String SAMU_B_ROUTING_KEY = "fr.health.samuB";
    private final String SAMU_B_MESSAGE_QUEUE = SAMU_B_ROUTING_KEY + ".message";
    private final String SAMU_B_INFO_QUEUE = SAMU_B_ROUTING_KEY + ".info";
    private final String SAMU_B_ERROR_QUEUE = SAMU_B_ROUTING_KEY + ".error";
    private final String SAMU_A_ROUTING_KEY = "fr.health.samuA";
    private final String SAMU_A_MESSAGE_QUEUE = SAMU_A_ROUTING_KEY + ".message";
    private final String SAMU_A_INFO_QUEUE = SAMU_A_ROUTING_KEY + ".info";
    private final String SAMU_A_ERROR_QUEUE = SAMU_A_ROUTING_KEY + ".error";
    private final String INCONSISTENT_ROUTING_KEY = "fr.health.no-samu";
    private final String JSON = MessageProperties.CONTENT_TYPE_JSON;
    private final String XML = MessageProperties.CONTENT_TYPE_XML;

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
    @DisplayName("should send json message to the right exchange and routing key")
    public void shouldDispatchJsonToRightExchange() throws IOException {
        // generate input message and check that it has the expected content type
        Message receivedMessage = createMessage("EDXL-DE", JSON, SAMU_A_ROUTING_KEY);
        assertEquals(JSON, receivedMessage.getMessageProperties().getContentType());
        // dispatch message
        dispatcher.dispatch(receivedMessage);
        ArgumentCaptor<Message> argCaptor = ArgumentCaptor.forClass(Message.class);
        // assert that the message was sent to the right exchange with the right routing key exactly 1 time
        Mockito.verify(rabbitTemplate, times(1)).send(
                eq(DISTRIBUTION_EXCHANGE), eq(SAMU_B_MESSAGE_QUEUE), argCaptor.capture());
        // assert that the message has been converted according to the recipient preferences
        Message sentMessage = argCaptor.getValue();
        assertEquals(XML, sentMessage.getMessageProperties().getContentType());
        // assert that the message has the same content as the original one
        EdxlMessage publishedJSON = converter.deserializeJsonEDXL(new String(receivedMessage.getBody(), StandardCharsets.UTF_8));
        EdxlMessage sentXML = converter.deserializeXmlEDXL(new String(sentMessage.getBody(), StandardCharsets.UTF_8));
        assertEquals(publishedJSON, sentXML);

        CustomMessage custom = (CustomMessage) sentXML.getContentMessage();
        assertEquals("value", custom.getCustomContent().get("key").asText());
    }

    @Test
    @DisplayName("should send xml message to the right exchange and routing key")
    public void shouldDispatchXmlToRightExchange() throws IOException {
        // generate input message and check that it has the expected content type
        Message receivedMessage = createMessage("EDXL-DE", XML, SAMU_B_ROUTING_KEY);
        assertEquals(XML, receivedMessage.getMessageProperties().getContentType());
        // dispatch message
        dispatcher.dispatch(receivedMessage);
        ArgumentCaptor<Message> argCaptor = ArgumentCaptor.forClass(Message.class);
        // assert that the message was sent to the right exchange with the right routing key exactly 1 time
        Mockito.verify(rabbitTemplate, times(1)).send(
                eq(DISTRIBUTION_EXCHANGE), eq(SAMU_A_MESSAGE_QUEUE), argCaptor.capture());
        // assert that the message has been converted according to the recipient preferences
        Message sentMessage = argCaptor.getValue();
        assertEquals(JSON, sentMessage.getMessageProperties().getContentType());
        // assert that the message has the same content as the original one
        EdxlMessage publishedXML = converter.deserializeXmlEDXL(new String(receivedMessage.getBody(), StandardCharsets.UTF_8));
        EdxlMessage sentJSON = converter.deserializeJsonEDXL(new String(sentMessage.getBody(), StandardCharsets.UTF_8));
        assertEquals(publishedXML, sentJSON);

        CustomMessage custom = (CustomMessage) sentJSON.getContentMessage();
        assertEquals("value", custom.getCustomContent().get("key").asText());
    }

    @Test
    @DisplayName("should reset TTL if edxl dateTimeExpires is lower")
    public void shouldResetTTL() throws IOException {
        // get message and override dateTimeExpires field with sooner value
        Message base = createMessage("EDXL-DE",JSON, SAMU_A_ROUTING_KEY);
        EdxlMessage edxlMessage = converter.deserializeJsonEDXL(new String(base.getBody(), StandardCharsets.UTF_8));
        setCustomExpirationDate(edxlMessage, 2);
        Message customTTLMessage = new Message(converter.serializeJsonEDXL(edxlMessage).getBytes(), base.getMessageProperties());

        // before dispatch, the message has no expiration set
        assertNull(customTTLMessage.getMessageProperties().getExpiration());
        // method call
        dispatcher.dispatch(customTTLMessage);
        // we capture the forwarded message to ensure that it has been overwritten
        ArgumentCaptor<Message> argument = ArgumentCaptor.forClass(Message.class);
        Mockito.verify(rabbitTemplate, times(1)).send(
                eq(DISTRIBUTION_EXCHANGE), eq(SAMU_B_MESSAGE_QUEUE), argument.capture());

        // when calling rabbitTemplate.send(), the message has new expiration set
        assertNotNull(argument.getValue().getMessageProperties().getExpiration());
    }

    @Test
    @DisplayName("should send info to sender of DLQed message - expiration")
    public void handleDLQMessage() throws Exception {
        // we test that the message has been rejected after the DLQ listener has been called
        Message originalMessage = createMessage("EDXL-DE", JSON, SAMU_A_ROUTING_KEY);
        Message dlqMessage = applyRabbitmqDLQHeaders(originalMessage, "expired");
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> dispatcher.dispatchDLQ(dlqMessage));

        // we test that an error report has been sent with the correct error code
        assertErrorReportHasBeenSent(
                SAMU_A_INFO_QUEUE, ErrorCode.DEAD_LETTER_QUEUED,
                "fr.health.samuA_2608323d-507d-4cbf-bf74-52007f8124ea",
                "has been read from dead-letter-queue; reason was expired");
    }

    @Test
    @DisplayName("should not send info if info itself is DLQed")
    public void handleDLQInfo() throws Exception {
        Message originalInfo = createMessage("RS-INFO", JSON, SAMU_A_INFO_QUEUE);
        Message dlqMessage = applyRabbitmqDLQHeaders(originalInfo, "expired");

        assertDoesNotThrow(() -> dispatcher.dispatchDLQ(dlqMessage));
        Mockito.verify(rabbitTemplate, times(0)).send(
                eq(DISTRIBUTION_EXCHANGE), any(), any(Message.class));
    }

    @Test
    @DisplayName("malformed message should throw an exception")
    public void malformedMessagefailed() throws IOException {

        // we test that the message has been rejected if we can't parse it
        Message receivedMessage = createInvalidMessage("EDXL-DE/unparsable-content.json",  SAMU_A_ROUTING_KEY);
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> dispatcher.dispatch(receivedMessage));

        assertErrorReportHasBeenSent(SAMU_A_INFO_QUEUE, ErrorCode.UNRECOGNIZED_MESSAGE_FORMAT,
                "Could not parse message, invalid format. \n If you don't want to use HubSanté model" +
                        " for now, please use a \"customContent\" wrapper inside your message.");
    }

    @Test
    @DisplayName("message without content-type is rejected ")
    public void rejectMessageWithoutContentType() throws IOException {
        // we test that the message has been rejected if the content-type is not set
        Message receivedMessage = createMessage("EDXL-DE", null, SAMU_A_ROUTING_KEY);
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> dispatcher.dispatch(receivedMessage));

        // we test that an error report has been sent with the correct error code
        assertErrorReportHasBeenSent(SAMU_A_INFO_QUEUE, ErrorCode.NOT_ALLOWED_CONTENT_TYPE,
                "Unhandled Content-Type ! Message Content-Type should be set at 'application/json' or 'application/xml'");
    }

    @Test
    @DisplayName("message with unhandled content-type is rejected")
    public void rejectMessageWithUnhandledContentType() throws IOException {
        // we test that the message has been rejected if the content-type is neither json nor xml
        Message receivedMessage = createMessage("EDXL-DE", MessageProperties.DEFAULT_CONTENT_TYPE, SAMU_A_ROUTING_KEY);
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> dispatcher.dispatch(receivedMessage));

        // we test that an error report has been sent with the correct error code
        assertErrorReportHasBeenSent(SAMU_A_INFO_QUEUE, ErrorCode.NOT_ALLOWED_CONTENT_TYPE,
                "Unhandled Content-Type ! Message Content-Type should be set at 'application/json' or 'application/xml'");
    }

    @Test
    @DisplayName("message body inconsistent with content-type is rejected")
    public void rejectMessageWithInconsistentBody() throws IOException {
        // We create the AMQP message from the JSON file
        Message receivedMessage = createMessage("EDXL-DE", JSON, SAMU_A_ROUTING_KEY);
        // We override the content type to XML
        receivedMessage.getMessageProperties().setContentType(XML);
        // we test that the message has been rejected if the body is not consistent with the content-type
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
        Message receivedMessage = createMessage("EDXL-DE", JSON, INCONSISTENT_ROUTING_KEY);
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> dispatcher.dispatch(receivedMessage));

        // we test that an error report has been sent with the correct error code
        assertErrorReportHasBeenSent(INCONSISTENT_ROUTING_KEY + ".info", ErrorCode.SENDER_INCONSISTENCY,
                "message sender is fr.health.samuA", "received routing key is fr.health.no-samu");
    }

    @Test
    @DisplayName("should reject message without persistent delivery mode")
    public void rejectMessageWithoutPersistentDeliveryMode() throws IOException {
        Message receivedMessage = createMessage("EDXL-DE", JSON, SAMU_A_ROUTING_KEY);
        receivedMessage.getMessageProperties().setReceivedDeliveryMode(MessageDeliveryMode.NON_PERSISTENT);
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> dispatcher.dispatch(receivedMessage));

        // we test that an error report has been sent with the correct error code
        assertErrorReportHasBeenSent(SAMU_A_INFO_QUEUE, ErrorCode.DELIVERY_MODE_INCONSISTENCY,
                "fr.health.samuA_2608323d-507d-4cbf-bf74-52007f8124ea", "non-persistent delivery mode");
    }

    @Test
    @DisplayName("should reject message with invalid json EDXL envelope")
    public void invalidJsonEDXLFails() throws IOException {
        Message receivedMessage = createInvalidMessage("EDXL-DE/missing-EDXL-required-field.json", SAMU_A_ROUTING_KEY);
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> dispatcher.dispatch(receivedMessage));

        assertErrorReportHasBeenSent(SAMU_A_INFO_QUEUE, ErrorCode.INVALID_MESSAGE,
                "distributionID: is missing but it is required",
                "descriptor.explicitAddress.explicitAddressValue: is missing but it is required");
    }

    @Test
    @DisplayName("should reject message with invalid json content")
    public void invalidJsonContentFails() throws IOException {
        Message receivedMessage = createInvalidMessage("RC-EDA/invalid-RC-EDA-valid-EDXL.json",
                JSON, SAMU_A_ROUTING_KEY);
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> dispatcher.dispatch(receivedMessage));

        assertErrorReportHasBeenSent(SAMU_A_INFO_QUEUE, ErrorCode.INVALID_MESSAGE,
                "createdAt: is missing but it is required");
    }

    private void assertErrorReportHasBeenSent(String infoQueueName, ErrorCode errorCode, String... errorCause) throws JsonProcessingException {

        ArgumentCaptor<Message> argument = ArgumentCaptor.forClass(Message.class);
        Mockito.verify(rabbitTemplate, times(1)).send(
                eq(DISTRIBUTION_EXCHANGE), eq(infoQueueName), argument.capture());

        ErrorReport errorReport = getErrorReportFromMessage(converter, argument.getValue());
        assertEquals(errorCode, errorReport.getErrorCode());
        if (errorCause != null) {
            Arrays.stream(errorCause).forEach(cause -> assertTrue(errorReport.getErrorCause().contains(cause)));
        }
    }
}
