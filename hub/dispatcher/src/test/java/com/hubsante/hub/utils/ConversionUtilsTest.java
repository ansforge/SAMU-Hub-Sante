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

import static com.hubsante.hub.testsupport.HubTestScaffolding.aHub;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hubsante.hub.config.HubConfiguration;
import com.hubsante.hub.exception.UnroutableMessageException;
import com.hubsante.hub.utils.ConversionUtils.ConversionParametersDTO;
import com.hubsante.hub.utils.ConversionUtils.ConversionType;
import com.hubsante.model.edxl.Descriptor;
import com.hubsante.model.edxl.EdxlMessage;
import com.hubsante.model.edxl.ExplicitAddress;
import com.hubsante.model.report.ErrorWrapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Exercises the routing matrix against the real {@code config/clients.yaml}, so the expectations
 * below are readable next to that file rather than against stubbed registries.
 */
@DisplayName("ConversionUtils")
class ConversionUtilsTest {

    private static EdxlMessage message(String senderId, String recipientId) {
        EdxlMessage edxlMessage = new EdxlMessage();
        edxlMessage.setSenderID(senderId);
        edxlMessage.setDistributionID(senderId + "_1234");
        edxlMessage.setDescriptor(
                new Descriptor("fr-FR", new ExplicitAddress("hubex", recipientId)));
        edxlMessage.setContentFrom(new ErrorWrapper());
        return edxlMessage;
    }

    private static HubConfiguration hubOn(String vhost) {
        return aHub().onVhost(vhost).build().hubConfig();
    }

    // ─── the routing matrix ───────────────────────────────────────────────────

    @Nested
    @DisplayName("resolveConversionParameters")
    class ResolveConversionParameters {

        @ParameterizedTest(name = "{0} -> {1} on {2}")
        @CsvSource({
            // health -> health: convert when the recipient does not speak the current vhost version
            "fr.health.samuA,         fr.health.samuV3,        15-15_v2.1",
            "fr.health.samuA,         fr.health.samuA,         15-15_v2.1",
            // health -> CISU while already on the NexSIS vhost
            "fr.health.samuA,         fr.fire.sdisC,           15-nexsis_vactive",
        })
        @DisplayName("should not convert when the recipient already speaks the current version")
        void shouldNotConvert(String sender, String recipient, String vhost) {
            assertThat(
                            ConversionUtils.resolveConversionParameters(
                                    hubOn(vhost), message(sender, recipient)))
                    .isNull();
        }

        @ParameterizedTest(name = "{0} -> {1} on {2} yields {3} to {4}")
        @CsvSource({
            // health -> health, recipient stuck on an older version of the health perimeter
            "fr.health.samuA, fr.health.samuV1, 15-15_v2.1, HEALTH_VERSION_CONVERSION, 15-15_v1.5",
            "fr.health.samuA, fr.health.samuV2, 15-15_v2.1, HEALTH_VERSION_CONVERSION, 15-15_v2.0",
            // health -> CISU from a health vhost: transcode onto the NexSIS vhost
            "fr.health.samuA, fr.fire.sdisC,    15-15_v2.1, CISU_TRANSCODING,          15-nexsis_vactive",
            // health -> CISU from a CISU vhost: version conversion only
            "fr.health.samuV3-nexsis, fr.fire.sdisC, 15-nexsis_v1.9, CISU_VERSION_CONVERSION, 15-nexsis_vactive",
            // CISU -> health, recipient has no CISU perimeter: transcode down to its health vhost
            "fr.fire.sdisC,   fr.health.samuV3, 15-nexsis_vactive, CISU_TRANSCODING,     15-15_v2.1",
            // CISU -> health, recipient speaks CISU: stay on the CISU perimeter
            "fr.fire.sdisC,   fr.health.samuV3-nexsis, 15-nexsis_vactive, CISU_VERSION_CONVERSION, 15-nexsis_v1.9",
        })
        @DisplayName("should resolve the conversion type and target vhost")
        void shouldResolveConversion(
                String sender,
                String recipient,
                String vhost,
                ConversionType expectedType,
                String expectedTargetVhost) {
            ConversionParametersDTO parameters =
                    ConversionUtils.resolveConversionParameters(
                            hubOn(vhost), message(sender, recipient));

            assertThat(parameters).as("a conversion is expected").isNotNull();
            assertThat(parameters.conversionType()).isEqualTo(expectedType);
            assertThat(parameters.targetVhost()).isEqualTo(expectedTargetVhost);
        }

        @Test
        @DisplayName("should carry the model versions of the source and target vhosts")
        void shouldCarryModelVersions() {
            ConversionParametersDTO parameters =
                    ConversionUtils.resolveConversionParameters(
                            hubOn("15-15_v2.1"), message("fr.health.samuA", "fr.health.samuV1"));

            assertThat(parameters.sourceVersion()).as("15-15_v2.1 in clients.yaml").isEqualTo("v3");
            assertThat(parameters.targetVersion()).as("15-15_v1.5 in clients.yaml").isEqualTo("v1");
        }
    }

    // ─── the unchecked escapes ────────────────────────────────────────────────

    @Nested
    @DisplayName("unroutable combinations")
    class UnroutableCombinations {

        @Test
        @DisplayName(
                "should throw UnroutableMessageException from a perimeter with no NexSIS route")
        void shouldThrowWhenNoNexsisRouteFromPerimeter() {
            assertThatThrownBy(
                            () ->
                                    ConversionUtils.resolveConversionParameters(
                                            hubOn("15-smur_v1.7"),
                                            message("fr.health.samuA", "fr.fire.sdisC")))
                    .isInstanceOf(UnroutableMessageException.class)
                    .hasMessageContaining("Cannot route message to Nexsis from vhost 15-smur_v1.7");
        }

        /**
         * These two escapes are NOT {@link com.hubsante.hub.exception.AbstractHubException}s, so they
         * fall into {@code Dispatcher}'s generic catch and the sender gets no error report. Pinned
         * here as current behaviour — whether it should change is a separate decision.
         */
        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
            "fr.health.samuA, fr.other.partner",
            "fr.fire.sdisC,   fr.fire.sga",
            "fr.other.thing,  fr.health.samuA",
        })
        @DisplayName("should throw a bare RuntimeException when the routing type cannot be decided")
        void shouldThrowWhenRoutingTypeUndecidable(String sender, String recipient) {
            assertThatThrownBy(
                            () ->
                                    ConversionUtils.resolveConversionParameters(
                                            hubOn("15-15_v2.1"), message(sender, recipient)))
                    .isInstanceOf(RuntimeException.class)
                    .isNotInstanceOf(com.hubsante.hub.exception.AbstractHubException.class)
                    .hasMessageContaining("Cannot determine routing type");
        }

        @Test
        @DisplayName("should throw IllegalArgumentException for a vhost absent from the topology")
        void shouldThrowForUnknownVhost() {
            aHub().build(); // registers the TopologyRegistry singleton

            assertThatThrownBy(() -> ConversionUtils.getVHostMatchingModelVersion("15-unknown_v9"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .isNotInstanceOf(com.hubsante.hub.exception.AbstractHubException.class)
                    .hasMessageContaining("15-unknown_v9");
        }
    }

    // ─── vhost predicates and target selection ────────────────────────────────

    @Nested
    @DisplayName("vhost predicates")
    class VhostPredicates {

        @ParameterizedTest(name = "{0}: health={1} cisu={2}")
        @CsvSource({
            "15-15_v2.1,        true,  false",
            "15-15_v1.5,        true,  false",
            "15-nexsis_v1.9,    false, true",
            "15-nexsis_vactive, false, true",
            "15-smur_v1.7,      false, false",
        })
        @DisplayName("should classify a vhost by its perimeter")
        void shouldClassifyVhost(String vhost, boolean health, boolean cisu) {
            assertThat(ConversionUtils.isHealthVhost(vhost)).as("health").isEqualTo(health);
            assertThat(ConversionUtils.isCisuVhost(vhost)).as("cisu").isEqualTo(cisu);
        }

        @Test
        @DisplayName("should recognise only the configured NexSIS target as the NexSIS vhost")
        void shouldRecogniseNexsisVhost() {
            aHub().build();

            assertThat(ConversionUtils.isNexsisVhost("15-nexsis_vactive")).isTrue();
            assertThat(ConversionUtils.isNexsisVhost("15-nexsis_v1.9"))
                    .as("a CISU vhost that is not the hubex target")
                    .isFalse();
        }

        @Test
        @DisplayName("should select the latest vhost of the perimeter when conversion is needed")
        void shouldSelectLatestVhost() {
            HubConfiguration hubConfig = hubOn("15-15_v1.5");

            assertThat(
                            ConversionUtils.determineTargetVhostByPerimeter(
                                    hubConfig, "fr.health.samuA", "15-15"))
                    .as("samuA declares 1.5, 2.0 and 2.1; 1.5 is the current vhost")
                    .isNull();
            assertThat(
                            ConversionUtils.determineTargetVhostByPerimeter(
                                    hubConfig, "fr.health.samuV3", "15-15"))
                    .isEqualTo("15-15_v2.1");
        }

        @Test
        @DisplayName("should return no target when the recipient has no such perimeter")
        void shouldReturnNoTargetForUnknownPerimeter() {
            assertThat(
                            ConversionUtils.determineTargetVhostByPerimeter(
                                    hubOn("15-15_v2.1"), "fr.health.samuV3", "15-nexsis"))
                    .as("samuV3 declares no 15-nexsis perimeter")
                    .isNull();
        }

        @ParameterizedTest(name = "current {0} among {1} -> conversion needed = {2}")
        @CsvSource({
            "15-15_v2.1, '15-15_v1.5|15-15_v2.1', false",
            "15-15_v2.1, '15-15_v1.5|15-15_v2.0', true",
        })
        @DisplayName("should need a conversion only when the current vhost is not available")
        void shouldDetectConversionNeed(String current, String available, boolean expected) {
            assertThat(ConversionUtils.isConversionNeeded(current, available.split("\\|")))
                    .isEqualTo(expected);
        }
    }

    // ─── naming helpers ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("naming helpers")
    class NamingHelpers {

        @Test
        @DisplayName("should build the transfer exchange name from the source and target vhosts")
        void shouldBuildTransferExchangeName() {
            assertThat(ConversionUtils.buildTransferExchangeName("15-15_v1.5", "15-15_v2.0"))
                    .isEqualTo("transfer_15-15_v1.5_to_15-15_v2.0");
        }

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
            "15-15_v1.3,      15-15",
            "15-nexsis_v2,    15-nexsis",
            "backup_v2.0.1,   backup",
            "no-version-here, no-version-here",
            "'',              ''",
        })
        @DisplayName("should trim the version suffix from a vhost name")
        void shouldTrimVersionSuffix(String vhost, String expected) {
            assertThat(ConversionUtils.trimVersionSuffix(vhost)).isEqualTo(expected);
        }

        @Test
        @DisplayName("should pass null through the naming helpers")
        void shouldPassNullThrough() {
            assertThat(ConversionUtils.trimVersionSuffix(null)).isNull();
            assertThat(ConversionUtils.formatPerimeterVersionToVhost(null, "15-15")).isNull();
            assertThat(ConversionUtils.formatPerimeterVersionListToVhosts(null, "15-15")).isNull();
        }

        @Test
        @DisplayName("should format a perimeter version list into vhost names")
        void shouldFormatPerimeterVersionListToVhosts() {
            assertThat(
                            ConversionUtils.formatPerimeterVersionListToVhosts(
                                    new String[] {"1.5", "2.0"}, "15-15"))
                    .containsExactly("15-15_v1.5", "15-15_v2.0");
            assertThat(ConversionUtils.formatPerimeterVersionToVhost("1.7", "15-smur"))
                    .isEqualTo("15-smur_v1.7");
        }
    }
}
