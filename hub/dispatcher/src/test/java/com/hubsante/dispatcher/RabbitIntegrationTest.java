package com.hubsante.dispatcher;

import com.hubsante.model.edxl.EdxlMessage;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePropertiesBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class RabbitIntegrationTest extends RabbitIntegrationAbstract {

    @Test
    @DisplayName("message dispatched to exchange is received by a consumer listening to the right queue")
    public void dispatchTest() throws Exception {
        File edxlCisuCreateFile = new File(classLoader.getResource("messages/samuB_to_nexsis.xml").getFile());
        String json = Files.readString(edxlCisuCreateFile.toPath());

        MessageProperties properties = MessagePropertiesBuilder.newInstance().setContentType("application/xml").build();
        Message published = new Message(json.getBytes(StandardCharsets.UTF_8), properties);

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
    @DisplayName("samuB can't publish without topic permissions")
    public void unauthorizedConsumerTest() throws Exception {
        String p12Path = classLoader.getResource("config/certs/samuB/samuB.p12").getPath();
        RabbitTemplate samuB_client = getCustomRabbitTemplate(p12Path, "samuB");

        samuB_client.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                failed = true;
            }
        });

        File edxlCisuCreateFile = new File(classLoader.getResource("messages/samuB_to_nexsis.xml").getFile());
        String xml = Files.readString(edxlCisuCreateFile.toPath());

        MessageProperties properties = MessagePropertiesBuilder.newInstance().setContentType("application/xml").build();
        Message published = new Message(xml.getBytes(StandardCharsets.UTF_8), properties);
        samuB_client.sendAndReceive(
                HUBSANTE_EXCHANGE, "fr.health.samuA.out.message", published);

        assertTrue(failed);
    }
}
