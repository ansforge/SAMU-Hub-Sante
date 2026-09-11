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
package com.hubsante.hub.utils;

import static com.hubsante.hub.config.AmqpConfiguration.DLQ_EXPIRED_REASON;
import static com.hubsante.hub.config.AmqpConfiguration.DLQ_REASON;
import static com.hubsante.hub.config.AmqpConfiguration.ORIGINAL_ROUTING_KEY;
import static com.hubsante.hub.config.Constants.DISTRIBUTION_ID_UNAVAILABLE;
import static com.hubsante.hub.config.Constants.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hubsante.hub.config.HubConfiguration;
import com.hubsante.hub.exception.DeliveryModeInconsistencyException;
import com.hubsante.hub.exception.ExpiredBeforeDispatchMessageException;
import com.hubsante.hub.exception.InvalidDistributionIDException;
import com.hubsante.hub.exception.SenderInconsistencyException;
import com.hubsante.hub.exception.UnroutableMessageException;
import com.hubsante.model.edxl.Descriptor;
import com.hubsante.model.edxl.DistributionKind;
import com.hubsante.model.edxl.EdxlMessage;
import com.hubsante.model.edxl.ExplicitAddress;
import com.hubsante.model.report.ErrorWrapper;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;

@DisplayName("MessageUtils")
class MessageUtilsTest {

    private static final String SAMU_A = "fr.health.samuA";
    private static final String SDIS_C = "fr.fire.sdisC";
    private static final String DISTRIBUTION_ID = SAMU_A + "_1234";

    // ─── helpers ──────────────────────────────────────────────────────────────

    private static EdxlMessage edxl(String senderId, String recipientId, String distributionId) {
        EdxlMessage edxlMessage = new EdxlMessage();
        edxlMessage.setSenderID(senderId);
        edxlMessage.setDistributionID(distributionId);
        edxlMessage.setDescriptor(
                new Descriptor("fr-FR", new ExplicitAddress("hubex", recipientId)));
        edxlMessage.setContentFrom(new ErrorWrapper());
        return edxlMessage;
    }

    private static Message amqp(String receivedRoutingKey, String contentType) {
        MessageProperties properties = new MessageProperties();
        properties.setReceivedRoutingKey(receivedRoutingKey);
        properties.setContentType(contentType);
        properties.setReceivedDeliveryMode(MessageDeliveryMode.PERSISTENT);
        return new Message("{}".getBytes(StandardCharsets.UTF_8), properties);
    }

    // ─── checkSenderConsistency ───────────────────────────────────────────────

    @Nested
    @DisplayName("checkSenderConsistency")
    class CheckSenderConsistency {

        @Test
        @DisplayName("should pass when the routing key matches the senderID")
        void shouldPassWhenConsistent() {
            assertThatCode(
                            () ->
                                    MessageUtils.checkSenderConsistency(
                                            amqp(SAMU_A, MessageProperties.CONTENT_TYPE_JSON),
                                            edxl(SAMU_A, "fr.health.samuB", DISTRIBUTION_ID)))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should throw for a health sender whose routing key contradicts the senderID")
        void shouldThrowForInconsistentHealthSender() {
            assertThatThrownBy(
                            () ->
                                    MessageUtils.checkSenderConsistency(
                                            amqp(
                                                    "fr.health.someoneElse",
                                                    MessageProperties.CONTENT_TYPE_JSON),
                                            edxl(SAMU_A, "fr.health.samuB", DISTRIBUTION_ID)))
                    .isInstanceOf(SenderInconsistencyException.class)
                    .hasMessageContaining(DISTRIBUTION_ID)
                    .hasMessageContaining(SAMU_A)
                    .hasMessageContaining("fr.health.someoneElse");
        }

        @Test
        @DisplayName("should tolerate an inconsistent sender coming from a hubex partner")
        void shouldTolerateInconsistentHubexSender() {
            assertThatCode(
                            () ->
                                    MessageUtils.checkSenderConsistency(
                                            amqp("fr.fire.sga", MessageProperties.CONTENT_TYPE_XML),
                                            edxl(SDIS_C, "fr.health.samuB", "fr.fire.sdisC_1")))
                    .as("hubex senders are logged, not rejected")
                    .doesNotThrowAnyException();
        }
    }

    // ─── checkHealthActorIsInvolved ───────────────────────────────────────────

    @Nested
    @DisplayName("checkHealthActorIsInvolved")
    class CheckHealthActorIsInvolved {

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
            "fr.health.samuA, fr.health.samuB",
            "fr.health.samuA, fr.fire.sdisC",
            "fr.fire.sdisC,   fr.health.samuA",
        })
        @DisplayName("should pass when either actor is a health actor")
        void shouldPassWhenHealthActorInvolved(String sender, String recipient) {
            assertThatCode(
                            () ->
                                    MessageUtils.checkHealthActorIsInvolved(
                                            edxl(sender, recipient, sender + "_1")))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should throw when neither the sender nor the recipient is a health actor")
        void shouldThrowWhenNoHealthActorInvolved() {
            assertThatThrownBy(
                            () ->
                                    MessageUtils.checkHealthActorIsInvolved(
                                            edxl(SDIS_C, "fr.fire.sga", "fr.fire.sdisC_1")))
                    .isInstanceOf(UnroutableMessageException.class)
                    .hasMessageContaining("no health actor involved");
        }
    }

    // ─── checkDeliveryModeIsPersistent ────────────────────────────────────────

    @Nested
    @DisplayName("checkDeliveryModeIsPersistent")
    class CheckDeliveryModeIsPersistent {

        private Message nonPersistent(String routingKey) {
            Message message = amqp(routingKey, MessageProperties.CONTENT_TYPE_JSON);
            message.getMessageProperties()
                    .setReceivedDeliveryMode(MessageDeliveryMode.NON_PERSISTENT);
            return message;
        }

        @Test
        @DisplayName("should pass when the delivery mode is persistent")
        void shouldPassWhenPersistent() {
            assertThatCode(
                            () ->
                                    MessageUtils.checkDeliveryModeIsPersistent(
                                            amqp(SAMU_A, MessageProperties.CONTENT_TYPE_JSON),
                                            DISTRIBUTION_ID))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should throw for a health sender using a non-persistent delivery mode")
        void shouldThrowForHealthSender() {
            assertThatThrownBy(
                            () ->
                                    MessageUtils.checkDeliveryModeIsPersistent(
                                            nonPersistent(SAMU_A), DISTRIBUTION_ID))
                    .isInstanceOf(DeliveryModeInconsistencyException.class)
                    .hasMessageContaining("non-persistent delivery mode");
        }

        @Test
        @DisplayName("should tolerate a non-persistent delivery mode from a hubex partner")
        void shouldTolerateHubexSender() {
            assertThatCode(
                            () ->
                                    MessageUtils.checkDeliveryModeIsPersistent(
                                            nonPersistent("fr.fire.sga"), DISTRIBUTION_ID))
                    .doesNotThrowAnyException();
        }
    }

    // ─── checkDistributionIDFormat ────────────────────────────────────────────

    @Nested
    @DisplayName("checkDistributionIDFormat")
    class CheckDistributionIDFormat {

        @ParameterizedTest(name = "sender {0}, distributionID {1}")
        @CsvSource({
            "fr.health.samuA, fr.health.samuA_1234",
            "fr.health.samuA, fr.health.samuA_any-suffix_with_underscores",
        })
        @DisplayName("should accept a distributionID prefixed by the senderID")
        void shouldAcceptWellFormed(String sender, String distributionId) {
            assertThatCode(
                            () ->
                                    MessageUtils.checkDistributionIDFormat(
                                            edxl(sender, "fr.health.samuB", distributionId)))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest(name = "sender {0}, distributionID {1}")
        @CsvSource({
            "fr.health.samuA, fr.health.samuB_1234",
            "fr.health.samuA, 1234",
        })
        @DisplayName("should throw when the distributionID prefix is not the senderID")
        void shouldThrowOnMismatch(String sender, String distributionId) {
            assertThatThrownBy(
                            () ->
                                    MessageUtils.checkDistributionIDFormat(
                                            edxl(sender, "fr.health.samuB", distributionId)))
                    .isInstanceOf(InvalidDistributionIDException.class)
                    .hasMessageContaining("invalid distributionId format");
        }
    }

    // ─── checkMessageClassNameSupported ───────────────────────────────────────

    @Nested
    @DisplayName("checkMessageClassNameSupported")
    class CheckMessageClassNameSupported {

        /** The use case is the content message's own simple name — here {@code ErrorWrapper}. */
        private final EdxlMessage message = edxl(SAMU_A, "fr.health.samuB", DISTRIBUTION_ID);

        private HubConfiguration configSupporting(String... supported) {
            HubConfiguration hubConfig = mock(HubConfiguration.class);
            when(hubConfig.getSupportedMessages()).thenReturn(List.of(supported));
            return hubConfig;
        }

        @Test
        @DisplayName("should pass when the vhost supports the message class")
        void shouldPassWhenSupported() {
            assertThatCode(
                            () ->
                                    MessageUtils.checkMessageClassNameSupported(
                                            message, configSupporting("ErrorWrapper")))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should throw UnroutableMessageException when the class is not supported")
        void shouldThrowWhenUnsupported() {
            HubConfiguration hubConfig = configSupporting("SomethingElseWrapper");
            when(hubConfig.getVhost()).thenReturn("15-15_v1.5");

            assertThatThrownBy(
                            () -> MessageUtils.checkMessageClassNameSupported(message, hubConfig))
                    .isInstanceOf(UnroutableMessageException.class)
                    .hasMessageContaining("ErrorWrapper")
                    .hasMessageContaining("is not supported on the vhost 15-15_v1.5");
        }
    }

    // ─── overrideExpirationIfNeeded ───────────────────────────────────────────

    @Nested
    @DisplayName("overrideExpirationIfNeeded")
    class OverrideExpirationIfNeeded {

        private static final long ONE_DAY = 86400L;

        private EdxlMessage expiringIn(long seconds) {
            EdxlMessage edxlMessage = edxl(SAMU_A, "fr.health.samuB", DISTRIBUTION_ID);
            OffsetDateTime now = OffsetDateTime.now();
            edxlMessage.setDateTimeSent(now);
            edxlMessage.setDateTimeExpires(now.plusSeconds(seconds));
            return edxlMessage;
        }

        @Test
        @DisplayName("should override the AMQP expiration when the message expires before the TTL")
        void shouldOverrideWhenExpiringSooner() {
            MessageProperties properties = new MessageProperties();

            MessageUtils.overrideExpirationIfNeeded(expiringIn(60), properties, ONE_DAY);

            assertThat(properties.getExpiration())
                    .as("expiration in millis, close to the 60s the message asked for")
                    .isNotNull();
            assertThat(Long.parseLong(properties.getExpiration())).isBetween(55_000L, 60_000L);
        }

        @Test
        @DisplayName("should leave the expiration unset when the message outlives the TTL")
        void shouldNotOverrideWhenExpiringLater() {
            MessageProperties properties = new MessageProperties();

            MessageUtils.overrideExpirationIfNeeded(expiringIn(ONE_DAY * 2), properties, ONE_DAY);

            assertThat(properties.getExpiration()).isNull();
        }

        @Test
        @DisplayName("should reset an expiration header set by the client")
        void shouldResetClientSuppliedExpiration() {
            MessageProperties properties = new MessageProperties();
            properties.setExpiration("999999");

            MessageUtils.overrideExpirationIfNeeded(expiringIn(ONE_DAY * 2), properties, ONE_DAY);

            assertThat(properties.getExpiration())
                    .as("clients must use dateTimeExpires, not the AMQP header")
                    .isNull();
        }

        @Test
        @DisplayName("should throw when the message already expired")
        void shouldThrowWhenAlreadyExpired() {
            assertThatThrownBy(
                            () ->
                                    MessageUtils.overrideExpirationIfNeeded(
                                            expiringIn(-10), new MessageProperties(), ONE_DAY))
                    .isInstanceOf(ExpiredBeforeDispatchMessageException.class)
                    .hasMessageContaining("expired before reaching the recipient queue");
        }
    }

    // ─── routing and naming ───────────────────────────────────────────────────

    @Nested
    @DisplayName("recipient and queue naming")
    class RecipientAndQueueNaming {

        @Test
        @DisplayName("should read the recipient from the explicit address")
        void shouldReadRecipient() {
            assertThat(
                            MessageUtils.getRecipientID(
                                    edxl(SAMU_A, "fr.health.samuB", DISTRIBUTION_ID)))
                    .isEqualTo("fr.health.samuB");
        }

        @Test
        @DisplayName("should fall back to unknown when there is no explicit address")
        void shouldFallBackToUnknown() {
            assertThat(MessageUtils.getRecipientID(null)).isEqualTo(UNKNOWN);
            assertThat(MessageUtils.getRecipientID(new EdxlMessage())).isEqualTo(UNKNOWN);

            EdxlMessage noAddress = new EdxlMessage();
            noAddress.setDescriptor(new Descriptor());
            assertThat(MessageUtils.getRecipientID(noAddress)).isEqualTo(UNKNOWN);
        }

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
            "ACK, ack",
            "ERROR, info",
            "REPORT, message",
            "UPDATE, message",
            "CANCEL, message"
        })
        @DisplayName("should map the distribution kind to a queue suffix")
        void shouldMapQueueType(DistributionKind kind, String expected) {
            assertThat(MessageUtils.getQueueType(kind)).isEqualTo(expected);
        }

        @ParameterizedTest
        @EnumSource(DistributionKind.class)
        @DisplayName("should build the recipient queue name for every distribution kind")
        void shouldBuildRecipientQueueName(DistributionKind kind) {
            EdxlMessage edxlMessage = edxl(SAMU_A, "fr.health.samuB", DISTRIBUTION_ID);
            edxlMessage.setDistributionKind(kind);

            assertThat(MessageUtils.getRecipientQueueName(edxlMessage))
                    .isEqualTo("fr.health.samuB." + MessageUtils.getQueueType(kind));
        }

        @Test
        @DisplayName("should build the info queue name from a client id")
        void shouldBuildInfoQueueName() {
            assertThat(MessageUtils.getInfoQueueNameFromClientId(SAMU_A))
                    .isEqualTo("fr.health.samuA.info");
        }
    }

    // ─── content type detection ───────────────────────────────────────────────

    @Nested
    @DisplayName("content type detection")
    class ContentTypeDetection {

        @ParameterizedTest(name = "recipient {0}, useXML {1} -> {2}")
        @CsvSource({
            "fr.health.samuB, true,  true",
            "fr.health.samuB, false, false",
            "fr.health.samuB, , false",
            "fr.fire.sdisC,   , true",
            "fr.fire.sdisC,   false, false",
        })
        @DisplayName("should honour the client preference, defaulting fire recipients to XML")
        void shouldDecideConversionToXml(String recipientId, Boolean useXML, boolean expected) {
            assertThat(MessageUtils.convertToXML(recipientId, useXML)).isEqualTo(expected);
        }

        @Test
        @DisplayName("should detect JSON from the content type")
        void shouldDetectJson() {
            assertThat(MessageUtils.isJSON(amqp(SAMU_A, MessageProperties.CONTENT_TYPE_JSON)))
                    .isTrue();
            assertThat(MessageUtils.isJSON(amqp(SAMU_A, MessageProperties.CONTENT_TYPE_XML)))
                    .isFalse();
        }

        @Test
        @DisplayName("should detect XML from the content type")
        void shouldDetectXmlFromContentType() {
            assertThat(MessageUtils.isXML(amqp(SAMU_A, MessageProperties.CONTENT_TYPE_XML)))
                    .isTrue();
            assertThat(MessageUtils.isXML(amqp(SAMU_A, MessageProperties.CONTENT_TYPE_JSON)))
                    .isFalse();
        }

        @Test
        @DisplayName(
                "should treat a non-health or absent routing key as XML whatever the content type")
        void shouldDefaultNonHealthSendersToXml() {
            assertThat(MessageUtils.isXML(amqp("fr.fire.sga", MessageProperties.CONTENT_TYPE_JSON)))
                    .as("hubex partners speak XML")
                    .isTrue();
            assertThat(MessageUtils.isXML(amqp(null, MessageProperties.CONTENT_TYPE_JSON)))
                    .as("a null routing key falls into the same branch")
                    .isTrue();
        }

        @Test
        @DisplayName("should apply the same rule to returned messages")
        void shouldDetectXmlOnReturnedMessage() {
            Message json = amqp(SAMU_A, MessageProperties.CONTENT_TYPE_JSON);

            assertThat(MessageUtils.isXML(new ReturnedMessage(json, 312, "NO_ROUTE", "", SAMU_A)))
                    .isFalse();
            assertThat(
                            MessageUtils.isXML(
                                    new ReturnedMessage(json, 312, "NO_ROUTE", "", "fr.fire.sga")))
                    .isTrue();
        }
    }

    // ─── body helpers ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("body helpers")
    class BodyHelpers {

        @Test
        @DisplayName("should extract the distributionID from a JSON body")
        void shouldExtractFromJson() {
            assertThat(
                            MessageUtils.extractDistributionId(
                                    "{\"distributionID\": \"fr.health.a_1\"}"))
                    .isEqualTo("fr.health.a_1");
        }

        @Test
        @DisplayName("should extract the distributionID from an XML body")
        void shouldExtractFromXml() {
            assertThat(
                            MessageUtils.extractDistributionId(
                                    "<edxl><distributionID>fr.health.a_1</distributionID></edxl>"))
                    .isEqualTo("fr.health.a_1");
        }

        @Test
        @DisplayName("should report the distributionID as unavailable when absent")
        void shouldReportUnavailable() {
            assertThat(MessageUtils.extractDistributionId("{\"other\": \"value\"}"))
                    .isEqualTo(DISTRIBUTION_ID_UNAVAILABLE);
        }

        @Test
        @DisplayName("should hash the body deterministically")
        void shouldHashDeterministically() {
            Message message = amqp(SAMU_A, MessageProperties.CONTENT_TYPE_JSON);

            assertThat(MessageUtils.hashBody(message))
                    .as("SHA-256 of the body, base64 encoded")
                    .isEqualTo("RBNvo1WzZ4oRRq0W9+hknpT7T8If536DEMBg9hyq/4o=")
                    .isEqualTo(MessageUtils.hashBody(message));
        }

        @Test
        @DisplayName("should stringify the body as UTF-8")
        void shouldStringifyBody() {
            assertThat(
                            MessageUtils.stringifyBody(
                                    amqp(SAMU_A, MessageProperties.CONTENT_TYPE_JSON)))
                    .isEqualTo("{}");
        }
    }

    // ─── routing key headers and DLQ ──────────────────────────────────────────

    @Nested
    @DisplayName("routing key and DLQ headers")
    class RoutingKeyAndDlqHeaders {

        @Test
        @DisplayName("should read the sender from the received routing key")
        void shouldReadSender() {
            assertThat(MessageUtils.getSenderFromRoutingKey(amqp(SAMU_A, null))).isEqualTo(SAMU_A);
        }

        @Test
        @DisplayName("should return an empty sender when the routing key is absent")
        void shouldReturnEmptySender() {
            assertThat(MessageUtils.getSenderFromRoutingKey(amqp(null, null))).isEmpty();
        }

        @Test
        @DisplayName("should record the original routing key in a header")
        void shouldSetOriginalRoutingKeyHeader() {
            Message message = amqp(SAMU_A, MessageProperties.CONTENT_TYPE_JSON);

            MessageUtils.setOriginalRoutingKeyHeader(message);

            assertThat(message.getMessageProperties().<String>getHeader(ORIGINAL_ROUTING_KEY))
                    .isEqualTo(SAMU_A);
        }

        @Test
        @DisplayName("should not inhibit when the message carries no DLQ reason")
        void shouldNotInhibitWithoutReason() {
            assertThat(MessageUtils.isInhibitedErrorMessage(amqp(SAMU_A, null))).isFalse();
        }

        @Test
        @DisplayName("should not inhibit an expired message, so its sender still gets an info")
        void shouldNotInhibitExpired() {
            Message message = amqp(SAMU_A, null);
            message.getMessageProperties().setHeader(DLQ_REASON, DLQ_EXPIRED_REASON);

            assertThat(MessageUtils.isInhibitedErrorMessage(message)).isFalse();
        }

        @Test
        @DisplayName("should inhibit a message dead-lettered for any other reason")
        void shouldInhibitOtherReasons() {
            Message message = amqp(SAMU_A, null);
            message.getMessageProperties().setHeader(DLQ_REASON, "rejected");

            assertThat(MessageUtils.isInhibitedErrorMessage(message)).isTrue();
        }
    }
}
