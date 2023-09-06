package com.hubsante.dispatcher;

import com.hubsante.hub.service.ContentMessageHandler;
import com.hubsante.model.edxl.EdxlMessage;
import com.hubsante.model.report.ErrorCode;
import com.hubsante.model.report.ErrorReport;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;

import static com.hubsante.dispatcher.utils.MessageTestUtils.createMessage;
import static com.hubsante.dispatcher.utils.MessageTestUtils.setCustomExpirationDate;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class RabbitIntegrationTest extends RabbitIntegrationAbstract {

    private static long DISPATCHER_PROCESS_TIME = 1000;
    private static long DEFAULT_TTL = 5000;

    @Autowired
    private ContentMessageHandler contentMessageHandler;

    @Test
    @DisplayName("message dispatched to exchange is received by a consumer listening to the right queue")
    public void dispatchTest() throws Exception {
        Message published = createMessage("samuB_to_nexsis.xml", SAMU_B_OUTER_MESSAGE_ROUTING_KEY);
        RabbitTemplate samuB_client = getCustomRabbitTemplate(classLoader.getResource("config/certs/samuB/samuB.p12").getPath(), "samuB");
        samuB_client.send(HUBSANTE_EXCHANGE, SAMU_B_OUTER_MESSAGE_ROUTING_KEY, published);

        Thread.sleep(DISPATCHER_PROCESS_TIME);

        RabbitTemplate nexsis_client = getCustomRabbitTemplate(classLoader.getResource("config/certs/sdisZ/sdisZ.p12").getPath(), "sdisZ");
        Message received = nexsis_client.receive(SDIS_Z_MESSAGE_QUEUE);

        EdxlMessage publishedEdxl = converter.deserializeXmlEDXL(new String(published.getBody(), StandardCharsets.UTF_8));
        EdxlMessage receivedEdxl = converter.deserializeXmlEDXL(new String(received.getBody(), StandardCharsets.UTF_8));
        Assertions.assertEquals(publishedEdxl, receivedEdxl);
    }

    @Test
    @DisplayName("publish with unauthorized routing key fails")
    public void publishWithUnauthorizedRoutingKeyFails() throws Exception {
        String p12Path = classLoader.getResource("config/certs/samuB/samuB.p12").getPath();
        RabbitTemplate samuB_client = getCustomRabbitTemplate(p12Path, "samuB");

        samuB_client.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                failed = true;
            }
        });

        Message published = createMessage("samuB_to_nexsis.xml", SAMU_B_WRONG_OUTER_MESSAGE_ROUTING_KEY);
        samuB_client.send(HUBSANTE_EXCHANGE, SAMU_B_WRONG_OUTER_MESSAGE_ROUTING_KEY, published);
        Thread.sleep(DISPATCHER_PROCESS_TIME);

        assertTrue(failed);
    }

    @Test
    @DisplayName("publish with authorized but inconsistent routing key fails")
    public void publishWithAuthorizedButInconsistentRoutingKeyFails() throws Exception {
        String p12Path = classLoader.getResource("config/certs/samuB/samuB.p12").getPath();
        RabbitTemplate samuB_client = getCustomRabbitTemplate(p12Path, "samuB");

        samuB_client.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                failed = true;
            }
        });

        Message published = createMessage("samuA_to_nexsis.xml", SAMU_B_OUTER_MESSAGE_ROUTING_KEY);
        samuB_client.send(HUBSANTE_EXCHANGE, SAMU_A_OUTER_MESSAGE_ROUTING_KEY, published);
        Thread.sleep(DISPATCHER_PROCESS_TIME);

        assertTrue(failed);
    }

    @Test
    @DisplayName("expired message should be rejected")
    public void rejectExpiredMessage() throws Exception {
        Message published = createMessage("samuB_to_nexsis.xml", SAMU_B_OUTER_MESSAGE_ROUTING_KEY);
        RabbitTemplate samuB_client = getCustomRabbitTemplate(classLoader.getResource("config/certs/samuB/samuB.p12").getPath(), "samuB");
        samuB_client.send(HUBSANTE_EXCHANGE, SAMU_B_OUTER_MESSAGE_ROUTING_KEY, published);

        Thread.sleep(DISPATCHER_PROCESS_TIME + DEFAULT_TTL);
        assertRecipientDidNotReceive("sdisZ", SDIS_Z_MESSAGE_QUEUE);

        Message infoMsg = samuB_client.receive(SAMU_B_INFO_QUEUE);
        assertNotNull(infoMsg);

        String errorJson = new String(infoMsg.getBody());
        ErrorReport errorReport = (ErrorReport) contentMessageHandler.deserializeJsonMessage(errorJson);
        assertEquals(ErrorCode.DEAD_LETTER_QUEUED, errorReport.getErrorCode());
        assertEquals("Message samuB_2608323d-507d-4cbf-bf74-52007f8124ea has been read from dead-letter-queue; reason was expired",
                errorReport.getErrorCause());
    }

    @Test
    @DisplayName("message expired by publisher rule should be rejected")
    public void rejectExpiredMessageWithPublisherExpirationLowerThanHubTTL() throws Exception {
        Message published = createMessage("samuB_to_nexsis.xml", SAMU_B_OUTER_MESSAGE_ROUTING_KEY);
        published.getMessageProperties().setExpiration("100");
        RabbitTemplate samuB_client = getCustomRabbitTemplate(classLoader.getResource("config/certs/samuB/samuB.p12").getPath(), "samuB");
        samuB_client.send(HUBSANTE_EXCHANGE, SAMU_B_OUTER_MESSAGE_ROUTING_KEY, published);

        Thread.sleep(DISPATCHER_PROCESS_TIME);

        assertRecipientDidNotReceive("sdisZ", SDIS_Z_MESSAGE_QUEUE);
        Message infoMsg = samuB_client.receive(SAMU_B_INFO_QUEUE);
        assertNotNull(infoMsg);

        String errorJson = new String(infoMsg.getBody());
        ErrorReport errorReport = (ErrorReport) contentMessageHandler.deserializeJsonMessage(errorJson);

        assertEquals("Message samuB_2608323d-507d-4cbf-bf74-52007f8124ea has been read from dead-letter-queue; reason was expired",
                errorReport.getErrorCause());
    }

    @Test
    @DisplayName("message expired according to EDXL.dateTimeExpires should be rejected")
    public void rejectExpiredMessageWithEdxlDateTimeExpiresLowerThanHubTTL() throws Exception {
        Message source = createMessage("samuB_to_nexsis.xml", SAMU_B_OUTER_MESSAGE_ROUTING_KEY);
        EdxlMessage edxlMessage = converter.deserializeXmlEDXL(new String(source.getBody(), StandardCharsets.UTF_8));
        setCustomExpirationDate(edxlMessage, 100000);
        String xml = converter.serializeXmlEDXL(edxlMessage);
        Message published = new Message(xml.getBytes(), source.getMessageProperties());

        RabbitTemplate samuB_client = getCustomRabbitTemplate(classLoader.getResource("config/certs/samuB/samuB.p12").getPath(), "samuB");
        samuB_client.send(HUBSANTE_EXCHANGE, SAMU_B_OUTER_MESSAGE_ROUTING_KEY, published);

        Thread.sleep(DISPATCHER_PROCESS_TIME);
        assertRecipientDidNotReceive("sdisZ", SDIS_Z_MESSAGE_QUEUE);

        Message infoMsg = samuB_client.receive(SAMU_B_INFO_QUEUE);
        assertNotNull(infoMsg);

        String errorJson = new String(infoMsg.getBody());
        ErrorReport errorReport = (ErrorReport) contentMessageHandler.deserializeJsonMessage(errorJson);
        assertEquals(ErrorCode.EXPIRED_MESSAGE_BEFORE_ROUTING, errorReport.getErrorCode());
        assertEquals("Message samuB_2608323d-507d-4cbf-bf74-52007f8124ea has expired before reaching the recipient queue",
                errorReport.getErrorCause());
    }

    @Test
    @DisplayName("message without Content-type should be dlq")
    public void messageWithoutContentTypeIsDLQ() throws Exception {
        Message noContentTypeMsg = createMessage("samuB_to_nexsis.xml", null, SAMU_B_OUTER_MESSAGE_ROUTING_KEY);
        RabbitTemplate samuB_client = getCustomRabbitTemplate(classLoader.getResource("config/certs/samuB/samuB.p12").getPath(), "samuB");
        assertNull(samuB_client.receive(SAMU_B_INFO_QUEUE));
        samuB_client.send(HUBSANTE_EXCHANGE, SAMU_B_OUTER_MESSAGE_ROUTING_KEY, noContentTypeMsg);

        Thread.sleep(DISPATCHER_PROCESS_TIME);

        assertRecipientDidNotReceive("sdisZ", SDIS_Z_MESSAGE_QUEUE);
        Message infoMsg = samuB_client.receive(SAMU_B_INFO_QUEUE);
        assertNotNull(infoMsg);

        String errorJson = new String(infoMsg.getBody());
        ErrorReport errorReport = (ErrorReport) contentMessageHandler.deserializeJsonMessage(errorJson);
        assertEquals(ErrorCode.NOT_ALLOWED_CONTENT_TYPE, errorReport.getErrorCode());
        assertEquals("Unhandled Content-Type ! Message Content-Type should be set at 'application/json' or 'application/xml'",
                errorReport.getErrorCause());
    }

    private void assertRecipientDidNotReceive(String client, String queueName) throws Exception {
        RabbitTemplate nexsis_client = getCustomRabbitTemplate(
                classLoader.getResource("config/certs/" + client + "/" + client + ".p12").getPath(),
                client);
        Message received = nexsis_client.receive(queueName);
        assertNull(received);
    }
}
