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
import com.hubsante.model.EdxlHandler;
import com.hubsante.model.edxl.EdxlMessage;
import com.hubsante.model.report.ErrorCode;
import com.hubsante.model.report.ErrorReport;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.containers.Container;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Objects;

import static com.hubsante.hub.service.utils.MessageTestUtils.createInvalidMessage;
import static com.hubsante.hub.service.utils.MessageTestUtils.createMessage;
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
        Message published = createMessage("EDXL-DE", JSON, SAMU_A_ROUTING_KEY);
        RabbitTemplate samuA_publisher = getCustomRabbitTemplate(classLoader.getResource("config/certs/samuA/samuA.p12").getPath(), "samuA");
        samuA_publisher.send(HUBSANTE_EXCHANGE, SAMU_A_ROUTING_KEY, published);

        Thread.sleep(DISPATCHER_PROCESS_TIME);

        RabbitTemplate samuB_consumer = getCustomRabbitTemplate(classLoader.getResource("config/certs/samuB/samuB.p12").getPath(), "samuB");
        Message received = samuB_consumer.receive(SAMU_B_MESSAGE_QUEUE);

        EdxlMessage publishedEdxl = converter.deserializeJsonEDXL(new String(published.getBody(), StandardCharsets.UTF_8));
        EdxlMessage receivedEdxl = converter.deserializeXmlEDXL(new String(received.getBody(), StandardCharsets.UTF_8));
        Assertions.assertEquals(publishedEdxl, receivedEdxl);
    }

    @Test
    @DisplayName("publish with authorized but inconsistent routing key fails")
    public void publishWithAuthorizedButInconsistentRoutingKeyFails() throws Exception {
        String p12Path = classLoader.getResource("config/certs/samuA/samuA.p12").getPath();
        RabbitTemplate samuA_publisher = getCustomRabbitTemplate(p12Path, "samuA");

        samuA_publisher.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                failed = true;
            }
        });

        Message published = createMessage("EDXL-DE", JSON, SAMU_A_ROUTING_KEY);
        samuA_publisher.send(HUBSANTE_EXCHANGE, SAMU_B_ROUTING_KEY, published);
        Thread.sleep(DISPATCHER_PROCESS_TIME);

        assertTrue(failed);
    }

    @Test
    @DisplayName("publish to inexistent recipient")
    public void publishToInexistentRecipientFails() throws Exception {
        String p12Path = classLoader.getResource("config/certs/samuA/samuA.p12").getPath();
        RabbitTemplate samuA_publisher = getCustomRabbitTemplate(p12Path, "samuA");

        Message published = createInvalidMessage("EDXL-DE/inexistent-recipient-queue.json", SAMU_A_ROUTING_KEY);
        samuA_publisher.send(HUBSANTE_EXCHANGE, SAMU_A_ROUTING_KEY, published);
        Thread.sleep(DISPATCHER_PROCESS_TIME);

        assertErrorReportHasBeenReceived(samuA_publisher, SAMU_A_INFO_QUEUE, ErrorCode.UNROUTABLE_MESSAGE,
                "unable do deliver message to fr.health.inexistent.message",
                "312", "NO_ROUTE");
    }

    @Test
    @DisplayName("expired message should be rejected")
    public void rejectExpiredMessage() throws Exception {
        Message published = createMessage("EDXL-DE", JSON, SAMU_A_ROUTING_KEY);
        RabbitTemplate samuA_publisher = getCustomRabbitTemplate(classLoader.getResource("config/certs/samuA/samuA.p12").getPath(), "samuA");
        samuA_publisher.send(HUBSANTE_EXCHANGE, SAMU_A_ROUTING_KEY, published);

        Thread.sleep(DISPATCHER_PROCESS_TIME + DEFAULT_TTL);
        assertRecipientDidNotReceive("samuB", SAMU_B_MESSAGE_QUEUE);
        assertErrorReportHasBeenReceived(samuA_publisher, SAMU_A_INFO_QUEUE, ErrorCode.DEAD_LETTER_QUEUED,
                "fr.health.samuA_2608323d-507d-4cbf-bf74-52007f8124ea", "dead-letter-queue; reason was expired");
    }

    @Test
    @DisplayName("message rejected by client is DLQ handled")
    public void clientRejectsMessageToDLQ() throws Exception {
        Message published = createMessage("EDXL-DE", JSON, SAMU_A_ROUTING_KEY);
        RabbitTemplate samuA_publisher = getCustomRabbitTemplate(classLoader.getResource("config/certs/samuA/samuA.p12").getPath(), "samuA");
        RabbitTemplate samuB_consumer = getCustomRabbitTemplate(classLoader.getResource("config/certs/samuB/samuB.p12").getPath(), "samuB");

        samuA_publisher.send(HUBSANTE_EXCHANGE, SAMU_A_ROUTING_KEY, published);
        Thread.sleep(DISPATCHER_PROCESS_TIME);
        samuB_consumer.execute(channel -> {
            channel.basicConsume(SAMU_B_MESSAGE_QUEUE, false, (consumerTag, message) -> {
                channel.basicReject(message.getEnvelope().getDeliveryTag(), false);
            }, consumerTag -> {});
            return null;
        });
        Thread.sleep(DISPATCHER_PROCESS_TIME);
        assertRecipientDidNotReceive("samuB", SAMU_B_MESSAGE_QUEUE);
        assertErrorReportHasBeenReceived(samuA_publisher, SAMU_A_INFO_QUEUE, ErrorCode.DEAD_LETTER_QUEUED,
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
                (ErrorReport) edxlHandler.deserializeXmlEDXL(errorString).getFirstContentMessage() :
                (ErrorReport) edxlHandler.deserializeJsonEDXL(errorString).getFirstContentMessage();
        assertEquals(errorCode, errorReport.getErrorCode());
        Arrays.stream(errorCause).forEach(cause -> assertTrue(errorReport.getErrorCause().contains(cause)));
    }
}
