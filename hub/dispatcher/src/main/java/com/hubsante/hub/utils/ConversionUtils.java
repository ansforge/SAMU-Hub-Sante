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

import static com.hubsante.hub.config.AmqpConfiguration.TRANSFER_EXCHANGE_PREFIX;
import static com.hubsante.hub.config.Constants.*;
import static com.hubsante.hub.utils.MessageUtils.*;

import com.hubsante.hub.config.HubConfiguration;
import com.hubsante.hub.exception.UnroutableMessageException;
import com.hubsante.model.edxl.EdxlMessage;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConversionUtils {

    @Getter
    @RequiredArgsConstructor
    public enum ConversionType {
        HEALTH_VERSION_CONVERSION("HealthVersionConversion"),
        CISU_VERSION_CONVERSION("CISUVersionConversion"),
        CISU_TRANSCODING("CISUTranscoding");

        private final String type;
    }

    @Getter
    @RequiredArgsConstructor
    public enum RoutingType {
        SAMU_TO_SAMU("SamuToSamu"),
        CISU_TO_SAMU("CisuToSamu"),
        SAMU_TO_CISU("SamuToCisu");

        private final String type;
    }

    public record ConversionParametersDTO(
            EdxlMessage edxlMessage,
            String sourceVersion,
            String targetVersion,
            String targetVhost,
            ConversionType conversionType) {

        static ConversionParametersDTO forVhostConversion(
                EdxlMessage edxlMessage,
                String sourceVhost,
                String targetVhost,
                ConversionType conversionType) {
            return new ConversionParametersDTO(
                    edxlMessage,
                    getVHostMatchingModelVersion(sourceVhost),
                    getVHostMatchingModelVersion(targetVhost),
                    targetVhost,
                    conversionType);
        }
    }

    public static String buildTransferExchangeName(String sourceVHost, String targetVHost) {
        return TRANSFER_EXCHANGE_PREFIX + sourceVHost + "_to_" + targetVHost;
    }

    public static boolean isHealthVhost(String vhost) {
        return vhost.startsWith(Perimeter.HEALTH.getName());
    }

    public static boolean isCisuVhost(String vhost) {
        return vhost.startsWith(Perimeter.CISU.getName());
    }

    public static boolean isNexsisVhost(String vhost) {
        return NEXSIS_VHOST.equals(vhost);
    }

    public static String[] extractAvailableVhostsByPerimeter(
            HubConfiguration hubConfig, String recipientId, String perimeter) {
        String[] targetVersionsOnPerimeter =
                hubConfig.getClientVersionsForPerimeter(recipientId, perimeter);
        return formatPerimeterVersionListToVhosts(targetVersionsOnPerimeter, perimeter);
    }

    public static String determineTargetVhostByPerimeter(
            HubConfiguration hubConfig, String recipientId, String perimeter) {
        String currentVhost = hubConfig.getVhost();

        String[] availableVhosts =
                extractAvailableVhostsByPerimeter(hubConfig, recipientId, perimeter);

        if (availableVhosts == null || !isConversionNeeded(currentVhost, availableVhosts)) {
            return null;
        }

        return availableVhosts[availableVhosts.length - 1];
    }

    public static boolean isConversionNeeded(String currentVhost, String[] availableVhosts) {
        return !Arrays.asList(availableVhosts).contains(currentVhost);
    }

    public static ConversionParametersDTO resolveConversionParameters(
            HubConfiguration hubConfig, EdxlMessage edxlMessage) {
        return switch (determineRoutingType(edxlMessage)) {
            case SAMU_TO_SAMU -> resolveSamuToSamu(hubConfig, edxlMessage);
            case CISU_TO_SAMU -> resolveCisuToSamu(hubConfig, edxlMessage);
            case SAMU_TO_CISU -> resolveSamuToCisu(hubConfig, edxlMessage);
        };
    }

    private static ConversionParametersDTO resolveSamuToSamu(
            HubConfiguration hubConfig, EdxlMessage edxlMessage) {
        String recipientId = getRecipientID(edxlMessage);
        String currentVhost = hubConfig.getVhost();
        String perimeter = trimVersionSuffix(currentVhost);
        if (perimeter == null) {
            return null;
        }

        String targetVhost = determineTargetVhostByPerimeter(hubConfig, recipientId, perimeter);

        if (targetVhost == null) {
            return null;
        }

        return ConversionParametersDTO.forVhostConversion(
                edxlMessage,
                hubConfig.getVhost(),
                targetVhost,
                ConversionType.HEALTH_VERSION_CONVERSION);
    }

    private static ConversionParametersDTO resolveCisuToSamu(
            HubConfiguration hubConfig, EdxlMessage edxlMessage) {
        String recipientId = getRecipientID(edxlMessage);
        String currentVhost = hubConfig.getVhost();

        String[] availableCisuVhosts =
                extractAvailableVhostsByPerimeter(hubConfig, recipientId, Perimeter.CISU.getName());

        if (availableCisuVhosts != null && availableCisuVhosts.length > 0) {
            if (!isConversionNeeded(currentVhost, availableCisuVhosts)) {
                return null;
            }

            String latestCisuVhost = availableCisuVhosts[availableCisuVhosts.length - 1];
            return ConversionParametersDTO.forVhostConversion(
                    edxlMessage,
                    currentVhost,
                    latestCisuVhost,
                    ConversionType.CISU_VERSION_CONVERSION);
        }

        String targetHealthVhost =
                determineTargetVhostByPerimeter(hubConfig, recipientId, Perimeter.HEALTH.getName());

        if (targetHealthVhost == null) {
            return null;
        }

        return ConversionParametersDTO.forVhostConversion(
                edxlMessage, currentVhost, targetHealthVhost, ConversionType.CISU_TRANSCODING);
    }

    private static ConversionParametersDTO resolveSamuToCisu(
            HubConfiguration hubConfig, EdxlMessage edxlMessage) {
        String currentVhost = hubConfig.getVhost();

        if (isNexsisVhost(currentVhost)) {
            return null;
        }
        if (isCisuVhost(currentVhost)) {
            return ConversionParametersDTO.forVhostConversion(
                    edxlMessage,
                    currentVhost,
                    NEXSIS_VHOST,
                    ConversionType.CISU_VERSION_CONVERSION);
        }
        if (isHealthVhost(currentVhost)) {
            return ConversionParametersDTO.forVhostConversion(
                    edxlMessage, currentVhost, NEXSIS_VHOST, ConversionType.CISU_TRANSCODING);
        }
        throw unroutable(edxlMessage, "Cannot route message to Nexsis from vhost " + currentVhost);
    }

    private static UnroutableMessageException unroutable(EdxlMessage edxlMessage, String reason) {
        return new UnroutableMessageException(
                reason,
                edxlMessage.getDistributionID(),
                getRecipientID(edxlMessage),
                EdxlUtils.getUseCaseFromMessage(edxlMessage.getFirstContentMessage()));
    }

    private static RoutingType determineRoutingType(EdxlMessage edxlMessage) {
        String senderId = edxlMessage.getSenderID();
        String recipientId = getRecipientID(edxlMessage);

        if (isHealthActor(senderId)) {
            if (isHealthActor(recipientId)) {
                return RoutingType.SAMU_TO_SAMU;
            } else if (isNexsisActor(recipientId)) {
                return RoutingType.SAMU_TO_CISU;
            }
        } else if (isNexsisActor(senderId)) {
            if (isHealthActor(recipientId)) {
                return RoutingType.CISU_TO_SAMU;
            }
        }
        throw new RuntimeException(
                String.format(
                        "Cannot determine routing type from %s to %s", senderId, recipientId));
    }

    private static boolean isHealthActor(String actorId) {
        return actorId.startsWith(FR_HEALTH_PREFIX);
    }

    private static boolean isNexsisActor(String actorId) {
        return actorId.startsWith(FR_FIRE_PREFIX) || actorId.startsWith(FR_CISU_PREFIX);
    }

    public static String[] formatPerimeterVersionListToVhosts(
            String[] versions, String sourcePerimeter) {
        if (versions != null) {
            return Arrays.stream(versions)
                    .map(version -> formatPerimeterVersionToVhost(version, sourcePerimeter))
                    .toArray(String[]::new);
        }
        return null;
    }

    public static String formatPerimeterVersionToVhost(String version, String perimeter) {
        if (version != null) {
            return perimeter + "_v" + version;
        }
        return null;
    }

    public static String trimVersionSuffix(String input) {
        String VERSION_SUFFIX_REGEX = "_v[\\d\\.]+$";

        if (input == null) return null;
        return input.replaceFirst(VERSION_SUFFIX_REGEX, "");
    }

    public static String getVHostMatchingModelVersion(String vHost) {
        String modelVersion = CONVERSION_VHOST_MODEL.get(vHost);
        if (modelVersion == null) {
            throw new IllegalArgumentException(
                    "There is no model version associated with the host " + vHost);
        }
        return modelVersion;
    }
}
