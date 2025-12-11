package com.hubsante.hub.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.hubsante.hub.config.HubConfiguration;
import com.hubsante.model.EdxlHandler;
import com.hubsante.model.Validator;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePropertiesBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@RunWith(MockitoJUnitRunner.class)
public class LogIntegrityTest {

    private HubConfiguration hubConfiguration;
    private MeterRegistry meterRegistry;
    private ConversionHandler conversionHandler;
    private RabbitTemplate rabbitTemplate;

    private static final String V_HOST = "15-15_v2.1";
    private static final List<String> SUPPORTED_MESSAGES =
            List.of(
                    "ReferenceWrapper",
                    "ErrorWrapper",
                    "CreateCaseHealthWrapper",
                    "CreateCaseHealthUpdateWrapper",
                    "ResourcesInfoWrapper",
                    "ResourcesStatusWrapper",
                    "ResourcesRequestWrapper",
                    "ResourcesResponseWrapper",
                    "GeoPositionsUpdateWrapper",
                    "GeoResourcesRequestWrapper",
                    "GeoResourcesDetailsWrapper");
    private static final String SENDER_ID = "fr.health.test.samuA";
    private static final String RECIPIENT_ID = "fr.health.test.samuC";
    private static final String DISTRIBUTION_ID =
            "fr.health.test.samuA_c89f718b-73e0-4d0a-b8a9-12696bd49522";
    private static final String INPUT_HASH = "QMMrnn7DpO1+CUpTUpgNaU3XuyrBFP1PlWQJqBvApg4=";
    private static final HashMap<String, Boolean> DIRECT_CISU =
            new HashMap<>() {
                {
                    put(SENDER_ID, false);
                    put(RECIPIENT_ID, false);
                }
            };

    @BeforeEach
    void setup() {
        hubConfiguration = mock(HubConfiguration.class);
        meterRegistry = mock(MeterRegistry.class);
        conversionHandler = mock(ConversionHandler.class);
        rabbitTemplate = mock(RabbitTemplate.class);
        when(hubConfiguration.getVhost()).thenReturn(V_HOST);
        when(hubConfiguration.getDirectCisuPreferences()).thenReturn(DIRECT_CISU);
        when(hubConfiguration.getSupportedMessages()).thenReturn(SUPPORTED_MESSAGES);
        when(meterRegistry.counter(anyString(), any(String[].class)))
                .thenReturn(mock(Counter.class));
    }

    @Test
    void dispatchLogsHashWhenReceivingMessage() throws Exception {
        // Arrange: set up MessageHandler and Dispatcher with a ListAppender to capture logs
        Logger logger = (Logger) LoggerFactory.getLogger(MessageHandler.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        EdxlHandler edxlHandler = new EdxlHandler();
        XmlMapper xmlMapper = new XmlMapper();
        Validator validator = new Validator();
        ObjectMapper jsonMapper = new ObjectMapper();

        MessageHandler messageHandler =
                new MessageHandler(
                        rabbitTemplate,
                        edxlHandler,
                        hubConfiguration,
                        validator,
                        meterRegistry,
                        xmlMapper,
                        jsonMapper,
                        conversionHandler);

        Dispatcher dispatcher =
                new Dispatcher(
                        messageHandler,
                        rabbitTemplate,
                        edxlHandler,
                        xmlMapper,
                        jsonMapper,
                        conversionHandler,
                        hubConfiguration);

        // Arrange: Prepare a JSON AMQP message from input file
        MessageProperties props =
                MessagePropertiesBuilder.newInstance()
                        .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                        .build();
        props.setReceivedRoutingKey(SENDER_ID);
        props.setReceivedDeliveryMode(MessageDeliveryMode.PERSISTENT);
        byte[] body =
                this.getClass()
                        .getClassLoader()
                        .getResourceAsStream("sample/input_integrity_message.json")
                        .readAllBytes();
        Message amqpMessage = new Message(body, props);

        // Act: call dispatch
        dispatcher.dispatch(amqpMessage);

        // Assert: check that the first log found in logger contains expected message and hash
        var logs = listAppender.list;
        assertFalse(logs.isEmpty());
        var receivedLogWithHash = logs.getFirst();
        String expectedMessage =
                String.format(
                        " [x] Received Report from '%s': message with distributionId %s and hashed value %s",
                        SENDER_ID, DISTRIBUTION_ID, INPUT_HASH);
        assertEquals(Level.INFO, receivedLogWithHash.getLevel());
        assertTrue(receivedLogWithHash.getMessage().contains(expectedMessage));
    }
}
