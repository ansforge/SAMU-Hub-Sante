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
package com.hubsante.hub.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.hubsante.hub.config.Constants;
import com.hubsante.model.report.ErrorCode;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

/**
 * Pins the contract every hub exception must honour: the {@link ErrorCode} the client will see, the
 * distributionID the error report references, and the AMQP reject-without-requeue semantics.
 */
@DisplayName("Hub exceptions")
class HubExceptionsTest {

    private static final String MESSAGE = "something went wrong";
    private static final String DISTRIBUTION_ID = "fr.health.samuA_1234";
    private static final String RECIPIENT_ID = "fr.health.samuB";
    private static final String MESSAGE_TYPE = "ErrorWrapper";

    /** Exceptions carrying only a message and a distributionID. */
    static Stream<Arguments> shortFormExceptions() {
        return Stream.of(
                arguments(
                        new ConversionException(MESSAGE, DISTRIBUTION_ID),
                        ErrorCode.INVALID_MESSAGE),
                arguments(
                        new DeliveryModeInconsistencyException(MESSAGE, DISTRIBUTION_ID),
                        ErrorCode.DELIVERY_MODE_INCONSISTENCY),
                arguments(
                        new HubPersistenceException(MESSAGE, DISTRIBUTION_ID),
                        ErrorCode.UNROUTABLE_MESSAGE),
                arguments(
                        new NotAllowedContentTypeException(MESSAGE, DISTRIBUTION_ID),
                        ErrorCode.NOT_ALLOWED_CONTENT_TYPE),
                arguments(
                        new SchemaNotFoundException(MESSAGE, DISTRIBUTION_ID),
                        ErrorCode.SCHEMA_NOT_FOUND),
                arguments(
                        new SchemaValidationException(MESSAGE, DISTRIBUTION_ID),
                        ErrorCode.INVALID_MESSAGE),
                arguments(
                        new UnrecognizedMessageFormatException(MESSAGE, DISTRIBUTION_ID),
                        ErrorCode.UNRECOGNIZED_MESSAGE_FORMAT));
    }

    /** Exceptions also carrying the recipient and the message type. */
    static Stream<Arguments> fullFormExceptions() {
        return Stream.of(
                arguments(
                        new ConversionException(
                                MESSAGE, DISTRIBUTION_ID, RECIPIENT_ID, MESSAGE_TYPE),
                        ErrorCode.INVALID_MESSAGE),
                arguments(
                        new DeadLetteredMessageException(
                                MESSAGE, DISTRIBUTION_ID, RECIPIENT_ID, MESSAGE_TYPE),
                        ErrorCode.DEAD_LETTER_QUEUED),
                arguments(
                        new ExpiredBeforeDispatchMessageException(
                                MESSAGE, DISTRIBUTION_ID, RECIPIENT_ID, MESSAGE_TYPE),
                        ErrorCode.EXPIRED_MESSAGE_BEFORE_ROUTING),
                arguments(
                        new HubPersistenceException(
                                MESSAGE, DISTRIBUTION_ID, RECIPIENT_ID, MESSAGE_TYPE),
                        ErrorCode.UNROUTABLE_MESSAGE),
                arguments(
                        new InvalidDistributionIDException(
                                MESSAGE, DISTRIBUTION_ID, RECIPIENT_ID, MESSAGE_TYPE),
                        ErrorCode.INVALID_MESSAGE),
                arguments(
                        new SenderInconsistencyException(
                                MESSAGE, DISTRIBUTION_ID, RECIPIENT_ID, MESSAGE_TYPE),
                        ErrorCode.SENDER_INCONSISTENCY),
                arguments(
                        new UnroutableMessageException(
                                MESSAGE, DISTRIBUTION_ID, RECIPIENT_ID, MESSAGE_TYPE),
                        ErrorCode.UNROUTABLE_MESSAGE));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("shortFormExceptions")
    @DisplayName("should carry its error code and default the recipient and type to unknown")
    void shouldHonourShortFormContract(AbstractHubException exception, ErrorCode expectedCode) {
        assertThat(exception.getErrorCode()).as("error code").isEqualTo(expectedCode);
        assertThat(exception.getMessage()).contains(MESSAGE);
        assertThat(exception.getReferencedDistributionID()).isEqualTo(DISTRIBUTION_ID);
        assertThat(exception.getRecipientId()).isEqualTo(Constants.UNKNOWN);
        assertThat(exception.getMessageType()).isEqualTo(Constants.UNKNOWN);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fullFormExceptions")
    @DisplayName("should carry its error code, recipient and message type")
    void shouldHonourFullFormContract(AbstractHubException exception, ErrorCode expectedCode) {
        assertThat(exception.getErrorCode()).as("error code").isEqualTo(expectedCode);
        assertThat(exception.getMessage()).contains(MESSAGE);
        assertThat(exception.getReferencedDistributionID()).isEqualTo(DISTRIBUTION_ID);
        assertThat(exception.getRecipientId()).isEqualTo(RECIPIENT_ID);
        assertThat(exception.getMessageType()).isEqualTo(MESSAGE_TYPE);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource({"shortFormExceptions", "fullFormExceptions"})
    @DisplayName("should reject the message without requeueing it")
    void shouldRejectWithoutRequeue(AbstractHubException exception, ErrorCode ignored) {
        assertThat(exception)
                .as("the broker must not redeliver a message the hub already refused")
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
    }

    @Test
    @DisplayName("should prefix the cause for the two exceptions wrapping an internal service call")
    void shouldPrefixInternalServiceFailures() {
        assertThat(new ConversionException(MESSAGE, DISTRIBUTION_ID).getMessage())
                .isEqualTo(
                        "Error during internal call to Hub Santé conversion service: " + MESSAGE);
        assertThat(new HubPersistenceException(MESSAGE, DISTRIBUTION_ID).getMessage())
                .isEqualTo(
                        "Error during internal call to Hub Santé persistence service: " + MESSAGE);
    }

    @Test
    @DisplayName("should leave the message untouched for the other exceptions")
    void shouldNotPrefixOtherExceptions() {
        assertThat(new SchemaValidationException(MESSAGE, DISTRIBUTION_ID).getMessage())
                .isEqualTo(MESSAGE);
        assertThat(
                        new UnroutableMessageException(
                                        MESSAGE, DISTRIBUTION_ID, RECIPIENT_ID, MESSAGE_TYPE)
                                .getMessage())
                .isEqualTo(MESSAGE);
    }

    @Test
    @DisplayName("should keep ClientConfigurationException outside the client-facing hierarchy")
    void shouldKeepClientConfigurationExceptionSeparate() {
        ClientConfigurationException exception = new ClientConfigurationException(MESSAGE);

        assertThat(exception)
                .as("a broken clients.yaml is a startup fault, not a message the hub can report on")
                .isInstanceOf(RuntimeException.class)
                .isNotInstanceOf(AbstractHubException.class);
        assertThat(exception.getMessage()).isEqualTo(MESSAGE);
    }
}
