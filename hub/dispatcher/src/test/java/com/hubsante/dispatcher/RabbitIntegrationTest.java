package com.hubsante.dispatcher;

import com.hubsante.model.edxl.EdxlMessage;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.nio.charset.StandardCharsets;

import static com.hubsante.dispatcher.utils.MessageTestUtils.createMessage;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class RabbitIntegrationTest extends RabbitIntegrationAbstract {

    @Test
    @DisplayName("message dispatched to exchange is received by a consumer listening to the right queue")
    public void dispatchTest() throws Exception {
        Message published = createMessage("samuB_to_nexsis.xml", SAMU_B_OUTER_MESSAGE_ROUTING_KEY);
        RabbitTemplate samuB_client = getCustomRabbitTemplate(classLoader.getResource("config/certs/samuB/samuB.p12").getPath(), "samuB");
        samuB_client.sendAndReceive(HUBSANTE_EXCHANGE, SAMU_B_OUTER_MESSAGE_ROUTING_KEY, published);

        Thread.sleep(100);

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
        samuB_client.sendAndReceive(HUBSANTE_EXCHANGE, SAMU_B_WRONG_OUTER_MESSAGE_ROUTING_KEY, published);

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
        samuB_client.sendAndReceive(HUBSANTE_EXCHANGE, SAMU_A_OUTER_MESSAGE_ROUTING_KEY, published);

        assertTrue(failed);
    }

    @Test
    @DisplayName("expired message should be rejected")
    public void rejectExpiredMessage() throws Exception {
        Message published = createMessage("samuB_to_nexsis.xml", SAMU_B_OUTER_MESSAGE_ROUTING_KEY);
        RabbitTemplate samuB_client = getCustomRabbitTemplate(classLoader.getResource("config/certs/samuB/samuB.p12").getPath(), "samuB");
        samuB_client.sendAndReceive(HUBSANTE_EXCHANGE, SAMU_B_OUTER_MESSAGE_ROUTING_KEY, published);

        Thread.sleep(10000);

        RabbitTemplate nexsis_client = getCustomRabbitTemplate(classLoader.getResource("config/certs/sdisZ/sdisZ.p12").getPath(), "sdisZ");
        Message received = nexsis_client.receive(SDIS_Z_MESSAGE_QUEUE);
        assertNull(received);

        Message infoMsg = samuB_client.receive(SAMU_B_INFO_QUEUE);
        assertNotNull(infoMsg);
        String errorMsg = new String(infoMsg.getBody());
        assert(errorMsg.endsWith("has not been consumed on fr.fire.nexsis.sdisZ.message"));
    }

    @Test
    @DisplayName("message expired by publisher rule should be rejected")
    public void rejectExpiredMessageWithPublisherExpirationLowerThanHubTTL() throws Exception {
        Message published = createMessage("samuB_to_nexsis.xml", SAMU_B_OUTER_MESSAGE_ROUTING_KEY);
        published.getMessageProperties().setExpiration("100");
        RabbitTemplate samuB_client = getCustomRabbitTemplate(classLoader.getResource("config/certs/samuB/samuB.p12").getPath(), "samuB");
        samuB_client.sendAndReceive(HUBSANTE_EXCHANGE, SAMU_B_OUTER_MESSAGE_ROUTING_KEY, published);

        Thread.sleep(200);

        Message infoMsg = samuB_client.receive(SAMU_B_INFO_QUEUE);
        assertNotNull(infoMsg);
        String errorMsg = new String(infoMsg.getBody());
        assert(errorMsg.endsWith("has not been consumed on fr.fire.nexsis.sdisZ.message"));
    }

    @Test
    @DisplayName("message without Content-type should be dlq")
    public void messageWithoutContentTypeIsDLQ() throws Exception {
        Message noContentTypeMsg = createMessage("samuB_to_nexsis.xml", null, SAMU_B_OUTER_MESSAGE_ROUTING_KEY);
        RabbitTemplate samuB_client = getCustomRabbitTemplate(classLoader.getResource("config/certs/samuB/samuB.p12").getPath(), "samuB");
        samuB_client.sendAndReceive(HUBSANTE_EXCHANGE, SAMU_B_OUTER_MESSAGE_ROUTING_KEY, noContentTypeMsg);

        Thread.sleep(200);

        Message infoMsg = samuB_client.receive(SAMU_B_INFO_QUEUE);
        assertNotNull(infoMsg);
        String errorMsg = new String(infoMsg.getBody());
        assertEquals("Unhandled Content-Type ! Message Content-Type should be set at 'application/json' or 'application/xml'", errorMsg);
    }
}
