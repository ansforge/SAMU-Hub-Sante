/**
 * Copyright © 2023-2026 Agence du Numerique en Sante (ANS)
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

import static com.hubsante.hub.testsupport.MessageTestUtils.createInvalidMessage;
import static com.hubsante.hub.testsupport.MessageTestUtils.createMessage;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hubsante.model.EdxlHandler;
import com.hubsante.model.edxl.EdxlMessage;
import com.hubsante.model.report.Error;
import com.hubsante.model.report.ErrorCode;
import com.hubsante.model.report.ErrorWrapper;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class RabbitIntegrationTest extends RabbitIntegrationAbstract {

    @Autowired private EdxlHandler edxlHandler;

    @Test
    @DisplayName(
            "message dispatched to exchange is received by a consumer listening to the right queue")
    public void shouldDeliverToRecipientQueue() throws Exception {
        Message published = createMessage("EDXL-DE", JSON);
        RabbitTemplate samuA_publisher =
                getCustomRabbitTemplate(
                        classLoader.getResource("config/certs/samuA/samuA.p12").getPath(), "samuA");
        samuA_publisher.send(HUBSANTE_EXCHANGE, SAMU_A_ROUTING_KEY, published);

        Message received = awaitMessageOn(clientTemplate("samuB"), SAMU_B_MESSAGE_QUEUE);

        EdxlMessage publishedEdxl =
                converter.deserializeJsonEDXL(
                        new String(published.getBody(), StandardCharsets.UTF_8));
        EdxlMessage receivedEdxl =
                converter.deserializeXmlEDXL(
                        new String(received.getBody(), StandardCharsets.UTF_8));
        Assertions.assertEquals(publishedEdxl, receivedEdxl);
    }

    @Test
    @DisplayName("publish to inexistent recipient")
    public void publishToInexistentRecipientFails() throws Exception {
        String p12Path = classLoader.getResource("config/certs/samuA/samuA.p12").getPath();
        RabbitTemplate samuA_publisher = getCustomRabbitTemplate(p12Path, "samuA");

        Message published =
                createInvalidMessage("EDXL-DE/inexistent-recipient-queue.json", SAMU_A_ROUTING_KEY);
        samuA_publisher.send(HUBSANTE_EXCHANGE, SAMU_A_ROUTING_KEY, published);

        assertErrorHasBeenReceived(
                samuA_publisher,
                SAMU_A_INFO_QUEUE,
                ErrorCode.UNROUTABLE_MESSAGE,
                "fr.health.samuA_2608323d-507d-4cbf-bf74-52007f8124ea",
                "unable do deliver message to fr.health.inexistent.message",
                "312",
                "NO_ROUTE");
    }

    @Test
    @DisplayName("publish to inexistent recipient with XML")
    public void publishToInexistentRecipientFailsWithXML() throws Exception {
        String p12Path = classLoader.getResource("config/certs/samuB/samuB.p12").getPath();
        RabbitTemplate samuB_publisher = getCustomRabbitTemplate(p12Path, "samuB");

        Message published =
                createInvalidMessage("EDXL-DE/inexistent-recipient-queue.xml", SAMU_B_ROUTING_KEY);
        samuB_publisher.send(HUBSANTE_EXCHANGE, SAMU_B_ROUTING_KEY, published);

        assertErrorHasBeenReceived(
                samuB_publisher,
                SAMU_B_INFO_QUEUE,
                ErrorCode.UNROUTABLE_MESSAGE,
                "fr.health.samuB_2608323d-507d-4cbf-bf74-52007f8124ea",
                "312",
                "unable do deliver message to fr.health.inexistent.message",
                "NO_ROUTE");
    }

    @Test
    @DisplayName("expired message should be rejected")
    public void rejectExpiredMessage() throws Exception {
        Message published = createMessage("EDXL-DE", JSON);
        RabbitTemplate samuA_publisher =
                getCustomRabbitTemplate(
                        classLoader.getResource("config/certs/samuA/samuA.p12").getPath(), "samuA");
        samuA_publisher.send(HUBSANTE_EXCHANGE, SAMU_A_ROUTING_KEY, published);

        // the dead-letter report can only appear once the TTL has elapsed, so awaiting it is the
        // wait — asserting samuB got nothing before that would pass trivially
        assertErrorHasBeenReceived(
                samuA_publisher,
                SAMU_A_INFO_QUEUE,
                ErrorCode.DEAD_LETTER_QUEUED,
                "fr.health.samuA_2608323d-507d-4cbf-bf74-52007f8124ea",
                "fr.health.samuA_2608323d-507d-4cbf-bf74-52007f8124ea",
                "dead-letter-queue; reason was expired");

        assertRecipientDidNotReceive("samuB", SAMU_B_MESSAGE_QUEUE);
    }

    @Test
    @DisplayName("message rejected by client is DLQ handled")
    public void clientRejectsMessageToDLQ() throws Exception {
        Message published = createMessage("EDXL-DE", JSON);
        RabbitTemplate samuA_publisher =
                getCustomRabbitTemplate(
                        classLoader.getResource("config/certs/samuA/samuA.p12").getPath(), "samuA");
        RabbitTemplate samuB_consumer =
                getCustomRabbitTemplate(
                        classLoader.getResource("config/certs/samuB/samuB.p12").getPath(), "samuB");

        AtomicBoolean rejected = new AtomicBoolean(false);
        samuA_publisher.send(HUBSANTE_EXCHANGE, SAMU_A_ROUTING_KEY, published);
        samuB_consumer.execute(
                channel -> {
                    // The consumer must be cancelled before this channel goes back to the cache:
                    // a surviving consumer on SAMU_B_MESSAGE_QUEUE steals the messages that later
                    // tests expect on that queue.
                    String consumerTag =
                            channel.basicConsume(
                                    SAMU_B_MESSAGE_QUEUE,
                                    false,
                                    (tag, message) -> {
                                        channel.basicReject(
                                                message.getEnvelope().getDeliveryTag(), false);
                                        rejected.set(true);
                                    },
                                    tag -> {});
                    await("the client to reject the delivered message")
                            .atMost(DELIVERY_TIMEOUT)
                            .until(rejected::get);
                    channel.basicCancel(consumerTag);
                    return null;
                });
        assertRecipientDidNotReceive("samuB", SAMU_B_MESSAGE_QUEUE);
        assertRecipientDidNotReceive("samuA", SAMU_A_INFO_QUEUE);
    }

    private void assertRecipientDidNotReceive(String client, String queueName) throws Exception {
        assertNoMessageOn(clientTemplate(client), queueName);
    }

    private void assertErrorHasBeenReceived(
            RabbitTemplate rabbitTemplate,
            String infoQueueName,
            ErrorCode errorCode,
            String referenceDistributionId,
            String... errorCause)
            throws JsonProcessingException {

        Message infoMsg = awaitMessageOn(rabbitTemplate, infoQueueName);
        String errorString = new String(infoMsg.getBody());

        Error error =
                infoMsg.getMessageProperties()
                                .getContentType()
                                .equals(MessageProperties.CONTENT_TYPE_XML)
                        ? ((ErrorWrapper)
                                        edxlHandler
                                                .deserializeXmlEDXL(errorString)
                                                .getFirstContentMessage())
                                .getError()
                        : ((ErrorWrapper)
                                        edxlHandler
                                                .deserializeJsonEDXL(errorString)
                                                .getFirstContentMessage())
                                .getError();
        assertEquals(errorCode, error.getErrorCode());
        assertEquals(referenceDistributionId, error.getReferencedDistributionID());
        Arrays.stream(errorCause)
                .forEach(cause -> assertTrue(error.getErrorCause().contains(cause)));
    }
}
