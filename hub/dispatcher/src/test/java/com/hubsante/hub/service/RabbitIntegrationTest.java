package com.hubsante.hub.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hubsante.model.edxl.EdxlMessage;
import com.hubsante.model.report.ErrorCode;
import com.hubsante.model.report.ErrorReport;
import com.rabbitmq.client.Delivery;
import com.rabbitmq.client.GetResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static com.hubsante.hub.service.utils.MessageTestUtils.createMessage;
import static com.hubsante.hub.service.utils.MessageTestUtils.setCustomExpirationDate;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class RabbitIntegrationTest extends RabbitIntegrationAbstract {

    private static long DISPATCHER_PROCESS_TIME = 1000;
    private static long DEFAULT_TTL = 5000;

    @Autowired
    private EdxlHandler edxlHandler;

    @Test
    @DisplayName("message dispatched to exchange is received by a consumer listening to the right queue")
    public void dispatchTest() throws Exception {
        Message published = createMessage("valid/edxl_encapsulated/samuB_to_nexsis.xml", SAMU_B_OUTER_MESSAGE_ROUTING_KEY);
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

        Message published = createMessage("valid/edxl_encapsulated/samuB_to_nexsis.xml", SAMU_B_WRONG_OUTER_MESSAGE_ROUTING_KEY);
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

        Message published = createMessage("valid/edxl_encapsulated/samuA_to_nexsis.xml", SAMU_B_OUTER_MESSAGE_ROUTING_KEY);
        samuB_client.send(HUBSANTE_EXCHANGE, SAMU_A_OUTER_MESSAGE_ROUTING_KEY, published);
        Thread.sleep(DISPATCHER_PROCESS_TIME);

        assertTrue(failed);
    }

    @Test
    @DisplayName("publish to inexistent recipient")
    public void publishToInexistentRecipientFails() throws Exception {
        String p12Path = classLoader.getResource("config/certs/samuB/samuB.p12").getPath();
        RabbitTemplate samuB_client = getCustomRabbitTemplate(p12Path, "samuB");

        Message published = createMessage("routing/inexistent_recipient_queue.xml", SAMU_B_OUTER_MESSAGE_ROUTING_KEY);
        samuB_client.send(HUBSANTE_EXCHANGE, SAMU_B_OUTER_MESSAGE_ROUTING_KEY, published);
        Thread.sleep(DISPATCHER_PROCESS_TIME);

        assertErrorReportHasBeenReceived(samuB_client, SAMU_B_INFO_QUEUE, ErrorCode.UNROUTABLE_MESSAGE,
                "unable do deliver message to fr.health.inexistent.message",
                "312", "NO_ROUTE");
    }

    @Test
    @DisplayName("expired message should be rejected")
    public void rejectExpiredMessage() throws Exception {
        Message published = createMessage("valid/edxl_encapsulated/samuB_to_nexsis.xml", SAMU_B_OUTER_MESSAGE_ROUTING_KEY);
        RabbitTemplate samuB_client = getCustomRabbitTemplate(classLoader.getResource("config/certs/samuB/samuB.p12").getPath(), "samuB");
        samuB_client.send(HUBSANTE_EXCHANGE, SAMU_B_OUTER_MESSAGE_ROUTING_KEY, published);

        Thread.sleep(DISPATCHER_PROCESS_TIME + DEFAULT_TTL);
        assertRecipientDidNotReceive("sdisZ", SDIS_Z_MESSAGE_QUEUE);
        assertErrorReportHasBeenReceived(samuB_client, SAMU_B_INFO_QUEUE, ErrorCode.DEAD_LETTER_QUEUED,
                "fr.health.samuB_2608323d-507d-4cbf-bf74-52007f8124ea", "dead-letter-queue; reason was expired");
    }

    @Test
    @DisplayName("rejected info message should be dlq")
    public void rejectExpiredInfoMessage() throws Exception {
        Message published = createMessage("valid/edxl_encapsulated/samuA_to_nexsis.json", SAMU_A_OUTER_MESSAGE_ROUTING_KEY);
        RabbitTemplate samuA_client = getCustomRabbitTemplate(classLoader.getResource("config/certs/samuA/samuA.p12").getPath(), "samuA");
        samuA_client.send(HUBSANTE_EXCHANGE, SAMU_A_OUTER_MESSAGE_ROUTING_KEY, published);

        Thread.sleep(DISPATCHER_PROCESS_TIME + DEFAULT_TTL);
        assertRecipientDidNotReceive("sdisZ", SDIS_Z_MESSAGE_QUEUE);
        Thread.sleep(DISPATCHER_PROCESS_TIME + DEFAULT_TTL);
        assertRecipientDidNotReceive("samuA", SAMU_A_INFO_QUEUE);
    }

    @Test
    @DisplayName("message expired by publisher rule should be rejected")
    public void rejectExpiredMessageWithPublisherExpirationLowerThanHubTTL() throws Exception {
        Message published = createMessage("valid/edxl_encapsulated/samuB_to_nexsis.xml", SAMU_B_OUTER_MESSAGE_ROUTING_KEY);
        published.getMessageProperties().setExpiration("100");
        RabbitTemplate samuB_client = getCustomRabbitTemplate(classLoader.getResource("config/certs/samuB/samuB.p12").getPath(), "samuB");
        samuB_client.send(HUBSANTE_EXCHANGE, SAMU_B_OUTER_MESSAGE_ROUTING_KEY, published);

        Thread.sleep(DISPATCHER_PROCESS_TIME);

        assertRecipientDidNotReceive("sdisZ", SDIS_Z_MESSAGE_QUEUE);
        assertErrorReportHasBeenReceived(samuB_client, SAMU_B_INFO_QUEUE, ErrorCode.DEAD_LETTER_QUEUED,
                "fr.health.samuB_2608323d-507d-4cbf-bf74-52007f8124ea", "dead-letter-queue; reason was expired");
    }

    @Test
    @DisplayName("message expired according to EDXL.dateTimeExpires should be rejected")
    public void rejectExpiredMessageWithEdxlDateTimeExpiresLowerThanHubTTL() throws Exception {
        Message source = createMessage("valid/edxl_encapsulated/samuB_to_nexsis.xml", SAMU_B_OUTER_MESSAGE_ROUTING_KEY);
        EdxlMessage edxlMessage = converter.deserializeXmlEDXL(new String(source.getBody(), StandardCharsets.UTF_8));
        setCustomExpirationDate(edxlMessage, 100000);
        String xml = converter.serializeXmlEDXL(edxlMessage);
        Message published = new Message(xml.getBytes(), source.getMessageProperties());

        RabbitTemplate samuB_client = getCustomRabbitTemplate(classLoader.getResource("config/certs/samuB/samuB.p12").getPath(), "samuB");
        samuB_client.send(HUBSANTE_EXCHANGE, SAMU_B_OUTER_MESSAGE_ROUTING_KEY, published);

        Thread.sleep(DISPATCHER_PROCESS_TIME);
        assertRecipientDidNotReceive("sdisZ", SDIS_Z_MESSAGE_QUEUE);
        assertErrorReportHasBeenReceived(samuB_client, SAMU_B_INFO_QUEUE, ErrorCode.EXPIRED_MESSAGE_BEFORE_ROUTING,
                "fr.health.samuB_2608323d-507d-4cbf-bf74-52007f8124ea",  "has expired before reaching the recipient queue");
    }

    @Test
    @DisplayName("message without Content-type should be dlq")
    public void messageWithoutContentTypeIsDLQ() throws Exception {
        Message noContentTypeMsg = createMessage("valid/edxl_encapsulated/samuB_to_nexsis.xml", null, SAMU_B_OUTER_MESSAGE_ROUTING_KEY);
        RabbitTemplate samuB_client = getCustomRabbitTemplate(classLoader.getResource("config/certs/samuB/samuB.p12").getPath(), "samuB");
        assertNull(samuB_client.receive(SAMU_B_INFO_QUEUE));
        samuB_client.send(HUBSANTE_EXCHANGE, SAMU_B_OUTER_MESSAGE_ROUTING_KEY, noContentTypeMsg);

        Thread.sleep(DISPATCHER_PROCESS_TIME);

        assertRecipientDidNotReceive("sdisZ", SDIS_Z_MESSAGE_QUEUE);
        assertErrorReportHasBeenReceived(samuB_client, SAMU_B_INFO_QUEUE, ErrorCode.NOT_ALLOWED_CONTENT_TYPE,
                "Unhandled Content-Type ! Message Content-Type should be set at 'application/json' or 'application/xml'");
    }

    @Test
    @DisplayName("message rejected by client is DLQ handled")
    public void clientRejectsMessageToDLQ() throws Exception {
        Message published = createMessage("valid/edxl_encapsulated/samuA_to_nexsis.json", SAMU_A_OUTER_MESSAGE_ROUTING_KEY);
        RabbitTemplate sdisZ_client = getCustomRabbitTemplate(classLoader.getResource("config/certs/sdisZ/sdisZ.p12").getPath(), "sdisZ");
        RabbitTemplate samuA_client = getCustomRabbitTemplate(classLoader.getResource("config/certs/samuA/samuA.p12").getPath(), "samuA");

        samuA_client.send(HUBSANTE_EXCHANGE, SAMU_A_OUTER_MESSAGE_ROUTING_KEY, published);
        Thread.sleep(DISPATCHER_PROCESS_TIME);
        sdisZ_client.execute(channel -> {
            channel.basicConsume(SDIS_Z_MESSAGE_QUEUE, false, (consumerTag, message) -> {
                channel.basicReject(message.getEnvelope().getDeliveryTag(), false);
            }, consumerTag -> {});
            return null;
        });
        Thread.sleep(DISPATCHER_PROCESS_TIME);
        assertRecipientDidNotReceive("sdisZ", SDIS_Z_MESSAGE_QUEUE);
        assertErrorReportHasBeenReceived(samuA_client, SAMU_A_INFO_QUEUE, ErrorCode.DEAD_LETTER_QUEUED,
                "rejected");
    }

    private void assertRecipientDidNotReceive(String client, String queueName) throws Exception {
        RabbitTemplate nexsis_client = getCustomRabbitTemplate(
                classLoader.getResource("config/certs/" + client + "/" + client + ".p12").getPath(),
                client);
        Message received = nexsis_client.receive(queueName);
        assertNull(received);
    }

    private void assertErrorReportHasBeenReceived(RabbitTemplate rabbitTemplate, String infoQueueName,
                                                         ErrorCode errorCode, String... errorCause) throws JsonProcessingException {

        Message infoMsg = rabbitTemplate.receive(infoQueueName);
        assertNotNull(infoMsg);
        String errorString = new String(infoMsg.getBody());

        ErrorReport errorReport = infoMsg.getMessageProperties().getContentType().equals(MessageProperties.CONTENT_TYPE_XML) ?
                (ErrorReport) edxlHandler.deserializeXmlContentMessage(errorString) :
                (ErrorReport) edxlHandler.deserializeJsonContentMessage(errorString);
        assertEquals(errorCode, errorReport.getErrorCode());
        Arrays.stream(errorCause).forEach(cause -> assertTrue(errorReport.getErrorCause().contains(cause)));
    }
}
