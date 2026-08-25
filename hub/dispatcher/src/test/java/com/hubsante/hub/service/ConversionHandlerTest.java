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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hubsante.hub.exception.ConversionException;
import com.hubsante.hub.utils.ConversionUtils.ConversionParametersDTO;
import com.hubsante.hub.utils.ConversionUtils.ConversionType;
import com.hubsante.model.EdxlHandler;
import com.hubsante.model.edxl.Descriptor;
import com.hubsante.model.edxl.EdxlMessage;
import com.hubsante.model.edxl.ExplicitAddress;
import com.hubsante.model.report.ErrorWrapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.client.reactive.MockClientHttpRequest;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Drives a real {@link WebClient} over a canned {@code ExchangeFunction}, so the production
 * {@code retrieve()} / {@code bodyToMono()} / {@link
 * org.springframework.web.reactive.function.client.WebClientResponseException} machinery runs for
 * real instead of being faked by a deep mock chain.
 */
@DisplayName("ConversionHandler")
class ConversionHandlerTest {

    private static final String DISTRIBUTION_ID = "fr.health.samuA_1234";
    private static final String RECIPIENT_ID = "fr.health.samuB";
    private static final String MOCK_EDXL_JSON_STRING =
            "{\"distributionID\":\"" + DISTRIBUTION_ID + "\"}";

    private WebClient webClientReturning(HttpStatus status, String body, MediaType contentType) {
        return WebClient.builder()
                .exchangeFunction(
                        request -> {
                            ClientResponse.Builder response = ClientResponse.create(status);
                            if (contentType != null) {
                                response.header("Content-Type", contentType.toString());
                            }
                            return Mono.just(response.body(body).build());
                        })
                .build();
    }

    private WebClient webClientReturningJson(String body) {
        return webClientReturning(HttpStatus.OK, body, MediaType.APPLICATION_JSON);
    }

    private ConversionHandler handlerReturning(WebClient webClient) {
        return new ConversionHandler(webClient, new EdxlHandler());
    }

    private List<String> convert(ConversionHandler handler) {
        return handler.callConversionService(
                MOCK_EDXL_JSON_STRING,
                "v1",
                "v3",
                ConversionType.HEALTH_VERSION_CONVERSION,
                DISTRIBUTION_ID);
    }

    // ─── successful responses ─────────────────────────────────────────────────

    @Nested
    @DisplayName("callConversionService")
    class CallConversionService {

        @Test
        @DisplayName("should return every element of converted_messages, verbatim")
        void shouldReturnConvertedMessages() {
            ConversionHandler handler =
                    handlerReturning(
                            webClientReturningJson(
                                    "{\"converted_messages\":[{\"a\":1},{\"b\":\"two\"}]}"));

            assertThat(convert(handler)).containsExactly("{\"a\":1}", "{\"b\":\"two\"}");
        }

        @Test
        @DisplayName("should return an empty list when converted_messages is absent")
        void shouldReturnEmptyWhenAbsent() {
            ConversionHandler handler = handlerReturning(webClientReturningJson("{\"other\":1}"));

            assertThat(convert(handler)).isEmpty();
        }

        @Test
        @DisplayName("should return an empty list when converted_messages is not an array")
        void shouldReturnEmptyWhenNotAnArray() {
            ConversionHandler handler =
                    handlerReturning(webClientReturningJson("{\"converted_messages\":\"nope\"}"));

            assertThat(convert(handler)).isEmpty();
        }

        @Test
        @DisplayName("should return an empty list when converted_messages is an empty array")
        void shouldReturnEmptyForEmptyArray() {
            ConversionHandler handler =
                    handlerReturning(webClientReturningJson("{\"converted_messages\":[]}"));

            assertThat(convert(handler)).isEmpty();
        }

        @Test
        @DisplayName("should wrap an unparseable 200 body in a ConversionException")
        void shouldWrapUnparseableSuccessBody() {
            ConversionHandler handler = handlerReturning(webClientReturningJson("not json at all"));

            assertThatThrownBy(() -> convert(handler))
                    .isInstanceOf(ConversionException.class)
                    .hasMessageContaining("Failed to parse response from conversion service");
        }
    }

    // ─── HTTP error responses ─────────────────────────────────────────────────

    @Nested
    @DisplayName("callConversionService on an HTTP error")
    class CallConversionServiceOnHttpError {

        @Test
        @DisplayName("should surface the converter's own error field on a 4xx")
        void shouldSurfaceErrorField() {
            ConversionHandler handler =
                    handlerReturning(
                            webClientReturning(
                                    HttpStatus.BAD_REQUEST,
                                    "{\"error\":\"unsupported target version\"}",
                                    MediaType.APPLICATION_JSON));

            assertThatThrownBy(() -> convert(handler))
                    .isInstanceOf(ConversionException.class)
                    .hasMessageContaining("unsupported target version");
        }

        @Test
        @DisplayName("should fall back to the HTTP error when the body carries no error field")
        void shouldFallBackToHttpError() {
            ConversionHandler handler =
                    handlerReturning(
                            webClientReturning(
                                    HttpStatus.BAD_REQUEST,
                                    "{\"detail\":\"something\"}",
                                    MediaType.APPLICATION_JSON));

            assertThatThrownBy(() -> convert(handler))
                    .isInstanceOf(ConversionException.class)
                    .hasMessageContaining("400");
        }

        @Test
        @DisplayName("should wrap a non-JSON 5xx body rather than leaking the parse failure")
        void shouldWrapNonJsonErrorBody() {
            ConversionHandler handler =
                    handlerReturning(
                            webClientReturning(
                                    HttpStatus.INTERNAL_SERVER_ERROR,
                                    "<html>gateway down</html>",
                                    MediaType.TEXT_HTML));

            assertThatThrownBy(() -> convert(handler))
                    .isInstanceOf(ConversionException.class)
                    .hasMessageContaining("Failed to parse error response from conversion service");
        }
    }

    // ─── applyConversionRules ─────────────────────────────────────────────────

    @Nested
    @DisplayName("applyConversionRules")
    class ApplyConversionRules {

        private ConversionParametersDTO buildConversionParameters() {
            EdxlMessage edxlMessage = new EdxlMessage();
            edxlMessage.setSenderID("fr.health.samuA");
            edxlMessage.setDistributionID(DISTRIBUTION_ID);
            edxlMessage.setDescriptor(
                    new Descriptor("fr-FR", new ExplicitAddress("hubex", RECIPIENT_ID)));
            edxlMessage.setContentFrom(new ErrorWrapper());
            return new ConversionParametersDTO(
                    edxlMessage,
                    "v1",
                    "v3",
                    "15-15_v1.5",
                    ConversionType.HEALTH_VERSION_CONVERSION);
        }

        @Test
        @DisplayName("should return the converted messages on success")
        void shouldReturnConvertedMessages() throws Exception {
            ConversionHandler handler =
                    handlerReturning(
                            webClientReturningJson("{\"converted_messages\":[{\"a\":1}]}"));

            assertThat(handler.applyConversionRules(buildConversionParameters()))
                    .containsExactly("{\"a\":1}");
        }

        @Test
        @DisplayName(
                "should wrap a converter failure in a ConversionException carrying the context")
        void shouldWrapFailureWithContext() {
            ConversionHandler handler =
                    handlerReturning(
                            webClientReturning(
                                    HttpStatus.BAD_REQUEST,
                                    "{\"error\":\"invalid parameter\"}",
                                    MediaType.APPLICATION_JSON));

            assertThatThrownBy(() -> handler.applyConversionRules(buildConversionParameters()))
                    .isInstanceOfSatisfying(
                            ConversionException.class,
                            exception -> {
                                assertThat(exception.getReferencedDistributionID())
                                        .isEqualTo(DISTRIBUTION_ID);
                                assertThat(exception.getRecipientId()).isEqualTo(RECIPIENT_ID);
                                assertThat(exception.getMessageType()).isEqualTo("ErrorWrapper");
                            })
                    .hasMessageContaining("invalid parameter");
        }
    }

    // ─── the outgoing request ─────────────────────────────────────────────────

    /** Renders the request's BodyInserter into a mock request so the payload can be asserted. */
    private static String readBody(ClientRequest request) {
        MockClientHttpRequest mockRequest =
                new MockClientHttpRequest(request.method(), request.url());
        BodyInserter.Context context =
                new BodyInserter.Context() {
                    @Override
                    public List<HttpMessageWriter<?>> messageWriters() {
                        return ExchangeStrategies.withDefaults().messageWriters();
                    }

                    @Override
                    public Optional<ServerHttpRequest> serverRequest() {
                        return Optional.empty();
                    }

                    @Override
                    public Map<String, Object> hints() {
                        return Map.of();
                    }
                };
        request.body().insert(mockRequest, context).block();
        return mockRequest.getBodyAsString().block();
    }

    @Test
    @DisplayName("should post the edxl, both versions and the conversion type to /convert")
    void shouldBuildTheRequest() {
        AtomicReference<String> uri = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        WebClient webClient =
                WebClient.builder()
                        .exchangeFunction(
                                request -> {
                                    uri.set(request.url().getPath());
                                    body.set(readBody(request));
                                    return Mono.just(
                                            ClientResponse.create(HttpStatus.OK)
                                                    .header(
                                                            "Content-Type",
                                                            MediaType.APPLICATION_JSON_VALUE)
                                                    .body("{\"converted_messages\":[]}")
                                                    .build());
                                })
                        .build();

        convert(handlerReturning(webClient));

        assertThat(uri.get()).isEqualTo("/convert");
        assertThat(body.get())
                .contains(MOCK_EDXL_JSON_STRING)
                .contains("\"sourceVersion\": \"v1\"")
                .contains("\"targetVersion\": \"v3\"")
                .contains("\"type\": \"HealthVersionConversion\"");
    }
}
