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
package com.hubsante.hub.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.hubsante.hub.HubApplication;
import com.hubsante.hub.config.HubConfiguration;
import com.hubsante.hub.service.utils.MessageTestUtils;
import com.hubsante.model.EdxlHandler;
import com.hubsante.model.Validator;
import com.hubsante.model.custom.CustomMessage;
import com.hubsante.model.edxl.EdxlMessage;
import com.hubsante.model.report.ErrorCode;
import com.hubsante.model.report.Error;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
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
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.hubsante.hub.config.AmqpConfiguration.*;
import static com.hubsante.hub.config.Constants.*;
import static com.hubsante.hub.service.utils.MessageTestUtils.*;
import static com.hubsante.hub.service.utils.MetricsUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@ContextConfiguration(classes = HubApplication.class)
@SpringRabbitTest
@Slf4j
public class DispatcherTest {

    private RabbitTemplate rabbitTemplate = Mockito.mock(RabbitTemplate.class);

    @Autowired
    private EdxlHandler edxlHandler;
    @Autowired
    private HubConfiguration hubConfig;
    @Autowired
    private Validator validator;
    private MessageHandler messageHandler;
    private ConversionHandler conversionHandler;
    private WebClient conversionWebClient = Mockito.mock(WebClient.class);
    @Autowired
    private MeterRegistry registry;
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
    private final String SAMU_A_DISTRIBUTION_ID = "fr.health.samuA_2608323d-507d-4cbf-bf74-52007f8124ea";
    private final String SDIS_C_ROUTING_KEY = "fr.fire.sdisC";

    private final String TEST_VHOST = "default-vhost";
    private final String INCONSISTENT_ROUTING_KEY = "fr.health.no-samu";
    private final String JSON = MessageProperties.CONTENT_TYPE_JSON;
    private final String XML = MessageProperties.CONTENT_TYPE_XML;
    @Autowired
    private XmlMapper xmlMapper;
    @Autowired
    private ObjectMapper jsonMapper;

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry propertiesRegistry) {
        propertiesRegistry.add("client.preferences.file",
                () -> Objects.requireNonNull(classLoader.getResource("config/client.preferences.csv")));
        propertiesRegistry.add("hubsante.default.message.ttl", () -> 5);
        propertiesRegistry.add("dispatcher.vhost", () -> "default-vhost");
    }

    @PostConstruct
    public void init() {
        messageHandler = new MessageHandler(rabbitTemplate, edxlHandler, hubConfig, validator, registry, xmlMapper, jsonMapper);
        conversionHandler = Mockito.spy(new ConversionHandler(conversionWebClient));
        dispatcher = new Dispatcher(messageHandler, rabbitTemplate, edxlHandler, xmlMapper, jsonMapper, conversionHandler);
    }

    @BeforeEach
    public void cleanMetrics() {
        registry.forEachMeter(meter -> {
            if (meter.getId().getName().equalsIgnoreCase(DISPATCH_ERROR)) {
                registry.remove(meter);
            }
        });
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
        EdxlMessage publishedJSON = edxlHandler.deserializeJsonEDXL(new String(receivedMessage.getBody(), StandardCharsets.UTF_8));
        EdxlMessage sentXML = edxlHandler.deserializeXmlEDXL(new String(sentMessage.getBody(), StandardCharsets.UTF_8));
        assertEquals(publishedJSON, sentXML);

        CustomMessage custom = (CustomMessage) sentXML.getFirstContentMessage();
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
        EdxlMessage publishedXML = edxlHandler.deserializeXmlEDXL(new String(receivedMessage.getBody(), StandardCharsets.UTF_8));
        EdxlMessage sentJSON = edxlHandler.deserializeJsonEDXL(new String(sentMessage.getBody(), StandardCharsets.UTF_8));
        assertEquals(publishedXML, sentJSON);

        CustomMessage custom = (CustomMessage) sentJSON.getFirstContentMessage();
        assertEquals("value", custom.getCustomContent().get("key").asText());
    }

    @Test
    @DisplayName("should convert messages according to client preferences")
    public void shouldConvertMessageAccordingToUseXmlPreferences() throws IOException {
        // JSON -> XML direction
        Message receivedJsonMessage = createMessage("EDXL-DE", JSON, SAMU_A_ROUTING_KEY);
        assertEquals(JSON, receivedJsonMessage.getMessageProperties().getContentType());

        dispatcher.dispatch(receivedJsonMessage);

        ArgumentCaptor<Message> argCaptor = ArgumentCaptor.forClass(Message.class);
        Mockito.verify(rabbitTemplate, times(1)).send(
                eq(DISTRIBUTION_EXCHANGE), eq(SAMU_B_MESSAGE_QUEUE), argCaptor.capture());
        Message sentXmlMessage = argCaptor.getValue();
        assertEquals(XML, sentXmlMessage.getMessageProperties().getContentType());

        // XML -> JSON direction
        Message receivedXMLMessage = createMessage("EDXL-DE", XML, SAMU_B_ROUTING_KEY);
        assertEquals(XML, receivedXMLMessage.getMessageProperties().getContentType());

        dispatcher.dispatch(receivedXMLMessage);

        Mockito.verify(rabbitTemplate, times(1)).send(
                eq(DISTRIBUTION_EXCHANGE), eq(SAMU_A_MESSAGE_QUEUE), argCaptor.capture());
        assertEquals(JSON, argCaptor.getValue().getMessageProperties().getContentType());
    }

    @Test
    @DisplayName("should call conversion service for cisu messages")
    public void shouldCallConversionServiceForCisuMessages() throws IOException {
        // Create a message from SDIS
        Message baseFromSdis = createMessage("EDXL-DE", XML, SDIS_C_ROUTING_KEY);
        EdxlMessage edxlMessageFromSdis = edxlHandler.deserializeXmlEDXL(new String(baseFromSdis.getBody(), StandardCharsets.UTF_8));
        MessageTestUtils.setMessageConsistentWithRoutingKey(edxlMessageFromSdis, SDIS_C_ROUTING_KEY);
        Message fromFireMessage = new Message(edxlHandler.serializeXmlEDXL(edxlMessageFromSdis).getBytes(), baseFromSdis.getMessageProperties());

        // Mock the conversion service call
        doAnswer(invocation -> invocation.getArgument(0)).when(conversionHandler).callConversionService(anyString());

        // Test message from SDIS
        dispatcher.dispatch(fromFireMessage);

        // Verify conversion was called
        verify(conversionHandler, times(1)).callConversionService(anyString());

        // Reset mock
        reset(conversionHandler);
        doAnswer(invocation -> invocation.getArgument(0)).when(conversionHandler).callConversionService(anyString());

        // Create message to SDIS
        Message baseToSdis = createMessage("EDXL-DE", JSON, SAMU_A_ROUTING_KEY);
        EdxlMessage edxlMessageToSdis = edxlHandler.deserializeJsonEDXL(new String(baseToSdis.getBody(), StandardCharsets.UTF_8));
        edxlMessageToSdis.getDescriptor().getExplicitAddress().setExplicitAddressValue(SDIS_C_ROUTING_KEY);
        Message toFireMessage = new Message(edxlHandler.serializeJsonEDXL(edxlMessageToSdis).getBytes(), baseToSdis.getMessageProperties());

        // Test message to SDIS
        dispatcher.dispatch(toFireMessage);
        
        // Verify conversion was called again
        verify(conversionHandler, times(1)).callConversionService(anyString());
    }

    @Test
    @DisplayName("should not call conversion service for health messages")
    public void shouldNotCallConversionServiceForHealthMessages() throws IOException {
        // Create a message from and to health
        Message message = createMessage("EDXL-DE", JSON, SAMU_A_ROUTING_KEY);

        // Dispatch the message
        dispatcher.dispatch(message);

        // Verify that conversion service was never called
        verify(conversionHandler, never()).callConversionService(anyString());
    }

    @Test
    @DisplayName("should reset TTL if edxl dateTimeExpires is lower")
    public void shouldResetTTL() throws IOException {
        // get message and override dateTimeExpires field with sooner value
        Message base = createMessage("EDXL-DE",JSON, SAMU_A_ROUTING_KEY);
        EdxlMessage edxlMessage = edxlHandler.deserializeJsonEDXL(new String(base.getBody(), StandardCharsets.UTF_8));
        setCustomExpirationDate(edxlMessage, 2);
        Message customTTLMessage = new Message(edxlHandler.serializeJsonEDXL(edxlMessage).getBytes(), base.getMessageProperties());

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
        assertErrorHasBeenSent(
                SAMU_A_INFO_QUEUE, ErrorCode.DEAD_LETTER_QUEUED,
                SAMU_A_DISTRIBUTION_ID,
                "fr.health.samuA_2608323d-507d-4cbf-bf74-52007f8124ea",
                "has been read from dead-letter-queue; reason was expired");
    }

    @Test
    @DisplayName("should not send info if info itself is DLQed")
    public void handleDLQInfo() throws Exception {
        Message originalInfo = createMessage("custom-error", JSON, SAMU_A_INFO_QUEUE);
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

        assertErrorHasBeenSent(SAMU_A_INFO_QUEUE, ErrorCode.UNRECOGNIZED_MESSAGE_FORMAT, SAMU_A_DISTRIBUTION_ID,
                "An internal server error has occurred, please contact the administration team");
    }

    @Test
    @DisplayName("message without content-type is rejected ")
    public void rejectMessageWithoutContentType() throws IOException {
        // we test that the message has been rejected if the content-type is not set
        Message receivedMessage = createMessage("EDXL-DE", null, SAMU_A_ROUTING_KEY);
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> dispatcher.dispatch(receivedMessage));

        // we test that an error report has been sent with the correct error code
        assertErrorHasBeenSent(SAMU_A_INFO_QUEUE, ErrorCode.NOT_ALLOWED_CONTENT_TYPE, SAMU_A_DISTRIBUTION_ID,
                "Unhandled Content-Type ! Message Content-Type should be set at 'application/json' or 'application/xml'");
    }

    @Test
    @DisplayName("message with unhandled content-type is rejected")
    public void rejectMessageWithUnhandledContentType() throws IOException {
        // we test that the message has been rejected if the content-type is neither json nor xml
        Message receivedMessage = createMessage("EDXL-DE", MessageProperties.DEFAULT_CONTENT_TYPE, SAMU_A_ROUTING_KEY);
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> dispatcher.dispatch(receivedMessage));

        // we test that an error report has been sent with the correct error code
        assertErrorHasBeenSent(SAMU_A_INFO_QUEUE, ErrorCode.NOT_ALLOWED_CONTENT_TYPE, SAMU_A_DISTRIBUTION_ID,
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
        assertErrorHasBeenSent(SAMU_A_INFO_QUEUE, ErrorCode.INVALID_MESSAGE, SAMU_A_DISTRIBUTION_ID,
                "Something went wrong with the XSD Validator");
    }

    @Test
    @DisplayName("outer routing key inconsistent with sender ID")
    public void outerRoutingKeyInconsistentWithSenderId() throws IOException {
        // we test that the message has been rejected if the sender ID is not consistent with the outer routing key
        Message receivedMessage = createMessage("EDXL-DE", JSON, INCONSISTENT_ROUTING_KEY);
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> dispatcher.dispatch(receivedMessage));

        // we test that an error report has been sent with the correct error code
        assertErrorHasBeenSent(INCONSISTENT_ROUTING_KEY + ".info", ErrorCode.SENDER_INCONSISTENCY, SAMU_A_DISTRIBUTION_ID,
                "message sender is fr.health.samuA", "received routing key is fr.health.no-samu");
    }

    @Test
    @DisplayName("should reject message without persistent delivery mode")
    public void rejectMessageWithoutPersistentDeliveryMode() throws IOException {
        Message receivedMessage = createMessage("EDXL-DE", JSON, SAMU_A_ROUTING_KEY);
        receivedMessage.getMessageProperties().setReceivedDeliveryMode(MessageDeliveryMode.NON_PERSISTENT);
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> dispatcher.dispatch(receivedMessage));

        // we test that an error report has been sent with the correct error code
        assertErrorHasBeenSent(SAMU_A_INFO_QUEUE, ErrorCode.DELIVERY_MODE_INCONSISTENCY, SAMU_A_DISTRIBUTION_ID,
                "fr.health.samuA_2608323d-507d-4cbf-bf74-52007f8124ea", "non-persistent delivery mode");
    }

    @Test
    @DisplayName("should reject message with invalid json EDXL envelope")
    public void invalidJsonEDXLFails() throws IOException {
        Message receivedMessage = createInvalidMessage("EDXL-DE/missing-EDXL-required-field.json", SAMU_A_ROUTING_KEY);
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> dispatcher.dispatch(receivedMessage));

        assertErrorHasBeenSent(SAMU_A_INFO_QUEUE, ErrorCode.INVALID_MESSAGE, DISTRIBUTION_ID_UNAVAILABLE,
                "distributionID: is missing but it is required",
                "descriptor.explicitAddress.explicitAddressValue: is missing but it is required");
    }

    @Test
    @DisplayName("should reject message with invalid json content")
    public void invalidJsonContentFails() throws IOException {
        Message receivedMessage = createInvalidMessage("EDXL-DE/invalid-content-valid-envelope.json",
                JSON, SAMU_A_ROUTING_KEY);

        assertThrows(AmqpRejectAndDontRequeueException.class, () -> dispatcher.dispatch(receivedMessage));
        assertErrorHasBeenSent(SAMU_A_INFO_QUEUE, ErrorCode.INVALID_MESSAGE, SAMU_A_DISTRIBUTION_ID,
                "reference.invalid_key: is not defined in the schema and the schema does not allow additional properties");
    }
    @Test
    @DisplayName("should reject message with invalid xml content")
    public void invalidXmlContentFails() throws IOException {
        Message receivedMessage = createInvalidMessage("EDXL-DE/invalid-content-valid-envelope.xml",
                XML, SAMU_B_ROUTING_KEY);

        assertThrows(AmqpRejectAndDontRequeueException.class, () -> dispatcher.dispatch(receivedMessage));
        assertErrorHasBeenSent(SAMU_B_INFO_QUEUE, ErrorCode.INVALID_MESSAGE, "fr.health.samuB_2608323d-507d-4cbf-bf74-52007f8124ea",
                "Invalid content was found starting with element '{\"urn:emergency:cisu:2.0:reference\":reference}'.");
    }

    @Test
    @DisplayName("should increment counter")
    public void incrementMetricsCounter() throws IOException {
        Search errorOverall = targetCounter(registry, CLIENT_ID_TAG, SAMU_A_ROUTING_KEY, VHOST_TAG, TEST_VHOST);
        Search errorContentType = targetCounter(registry, REASON_TAG, ErrorCode.NOT_ALLOWED_CONTENT_TYPE.getStatusString(),
                CLIENT_ID_TAG, SAMU_A_ROUTING_KEY, VHOST_TAG, TEST_VHOST);
        Search errorDeliveryMode = targetCounter(registry, REASON_TAG, ErrorCode.DELIVERY_MODE_INCONSISTENCY.getStatusString(),
                CLIENT_ID_TAG, SAMU_A_ROUTING_KEY, VHOST_TAG, TEST_VHOST);

        assertNull(errorOverall.counter());
        assertNull(errorContentType.counter());
        assertNull(errorDeliveryMode.counter());

        Message noContentTypeMessage = createMessage("EDXL-DE", null, SAMU_A_ROUTING_KEY);
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> dispatcher.dispatch(noContentTypeMessage));

        assertEquals(1, getCurrentCount(errorContentType.counter()));
        assertNull(errorDeliveryMode.counter());
        assertEquals(1, getOverallCounterForClient(registry, SAMU_A_ROUTING_KEY));

        Message nonPersistentMessage = createMessage("EDXL-DE", JSON, SAMU_A_ROUTING_KEY);
        nonPersistentMessage.getMessageProperties().setReceivedDeliveryMode(MessageDeliveryMode.NON_PERSISTENT);
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> dispatcher.dispatch(nonPersistentMessage));

        assertEquals(1, getCurrentCount(errorContentType.counter()));
        assertEquals(1, getCurrentCount(errorDeliveryMode.counter()));
        assertEquals(2, getOverallCounterForClient(registry, SAMU_A_ROUTING_KEY));
    }

    private void assertErrorHasBeenSent(String infoQueueName, ErrorCode errorCode, String referencedDistributionId, String... errorCause) throws JsonProcessingException {

        ArgumentCaptor<Message> argument = ArgumentCaptor.forClass(Message.class);
        Mockito.verify(rabbitTemplate, times(1)).send(
                eq(DISTRIBUTION_EXCHANGE), eq(infoQueueName), argument.capture());

        Error error = getErrorFromMessage(edxlHandler, argument.getValue());
        assertEquals(errorCode, error.getErrorCode());
        assertEquals(referencedDistributionId, error.getReferencedDistributionID());
        if (errorCause != null) {
            Arrays.stream(errorCause).forEach(cause -> assertTrue(error.getErrorCause().contains(cause)));
        }
    }
}
