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
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConversionUtils {

    private static final boolean DEFAULT_DIRECT_CISU_PREFERENCE = false;

    public enum ConversionType {
        HEALTH_VERSION_CONVERSION("HealthVersionConversion"),
        CISU_VERSION_CONVERSION("CISUVersionConversion"),
        CISU_TRANSCODING("CISUTranscoding");

        private final String type;

        ConversionType(String name) {
            this.type = name;
        }

        public String getType() {
            return type;
        }
    }

    public enum RoutingType {
        SAMU_TO_SAMU("SamuToSamu"),
        CISU_TO_SAMU("CisuToSamu"),
        SAMU_TO_CISU("SamuToCisu");

        private final String type;

        RoutingType(String name) {
            this.type = name;
        }

        public String getType() {
            return type;
        }
    }

    public record ConversionParametersDTO(
            EdxlMessage edxlMessage,
            String sourceVersion,
            String targetVersion,
            String targetVhost,
            ConversionType conversionType) {}

    public static String buildExchangeDestination(String sourceVHost, String targetVHost) {
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
            HubConfiguration hubConfig, String recipientId, Perimeter perimeter) {
        String[] targetVersionsOnPerimeter =
                hubConfig.getClientVersionsForPerimeter(recipientId, perimeter.getName());
        return formatPerimeterVersionListToVhosts(targetVersionsOnPerimeter, perimeter.getName());
    }

    public static String determineTargetVhostByPerimeter(
            HubConfiguration hubConfig, String recipientId, Perimeter perimeter) {
        String currentVhost = hubConfig.getVhost();

        String[] availableVhosts =
                extractAvailableVhostsByPerimeter(hubConfig, recipientId, perimeter);

        if (availableVhosts == null) {
            return null;
        }

        if (!isConversionNeeded(currentVhost, availableVhosts)) {
            return null;
        }

        return availableVhosts[availableVhosts.length - 1];
    }

    public static Boolean isConversionNeeded(String currentVhost, String[] availableVhosts) {
        return !Arrays.asList(availableVhosts).contains(currentVhost);
    }

    public static ConversionParametersDTO determineConversionParameters(
            HubConfiguration hubConfig, EdxlMessage edxlMessage) {
        RoutingType routingType = determineRoutingType(edxlMessage);

        String recipientId = getRecipientID(edxlMessage);
        String currentVhost = hubConfig.getVhost();

        switch (routingType) {
            case SAMU_TO_SAMU:
                String targetVhost =
                        determineTargetVhostByPerimeter(hubConfig, recipientId, Perimeter.HEALTH);

                if (targetVhost == null) {
                    return null;
                }

                return new ConversionParametersDTO(
                        edxlMessage,
                        getVHostMatchingModelVersion(currentVhost),
                        getVHostMatchingModelVersion(targetVhost),
                        targetVhost,
                        ConversionType.HEALTH_VERSION_CONVERSION);
            case CISU_TO_SAMU:
                String[] availableCISUVhosts =
                        extractAvailableVhostsByPerimeter(hubConfig, recipientId, Perimeter.CISU);

                if (availableCISUVhosts != null && availableCISUVhosts.length > 0) {
                    if (!isConversionNeeded(currentVhost, availableCISUVhosts)) {
                        return null;
                    }

                    return new ConversionParametersDTO(
                            edxlMessage,
                            getVHostMatchingModelVersion(currentVhost),
                            getVHostMatchingModelVersion(
                                    availableCISUVhosts[availableCISUVhosts.length - 1]),
                            availableCISUVhosts[availableCISUVhosts.length - 1],
                            ConversionType.CISU_VERSION_CONVERSION);
                }

                String targetHealthVhost =
                        determineTargetVhostByPerimeter(hubConfig, recipientId, Perimeter.HEALTH);

                if (targetHealthVhost == null) {
                    return null;
                }

                return new ConversionParametersDTO(
                        edxlMessage,
                        getVHostMatchingModelVersion(currentVhost),
                        getVHostMatchingModelVersion(targetHealthVhost),
                        targetHealthVhost,
                        ConversionType.CISU_TRANSCODING);
            case SAMU_TO_CISU:
                if (isNexsisVhost(currentVhost)) {
                    return null;
                }
                if (isCisuVhost(currentVhost)) {
                    return new ConversionParametersDTO(
                            edxlMessage,
                            getVHostMatchingModelVersion(currentVhost),
                            getVHostMatchingModelVersion(NEXSIS_VHOST),
                            NEXSIS_VHOST,
                            ConversionType.CISU_VERSION_CONVERSION);
                }
                if (isHealthVhost(currentVhost)) {
                    return new ConversionParametersDTO(
                            edxlMessage,
                            getVHostMatchingModelVersion(currentVhost),
                            getVHostMatchingModelVersion(NEXSIS_VHOST),
                            NEXSIS_VHOST,
                            ConversionType.CISU_TRANSCODING);
                }
                throw new UnroutableMessageException(
                        "Cannot route message to Nexsis from vhost " + currentVhost,
                        edxlMessage.getDistributionID(),
                        recipientId,
                        EdxlUtils.getUseCaseFromMessage(edxlMessage.getFirstContentMessage()));
            default:
                throw new UnroutableMessageException(
                        "Unable to route message from "
                                + edxlMessage.getSenderID()
                                + " to "
                                + recipientId
                                + " on vhost "
                                + currentVhost,
                        edxlMessage.getDistributionID(),
                        recipientId,
                        EdxlUtils.getUseCaseFromMessage(edxlMessage.getFirstContentMessage()));
        }
    }

    private static RoutingType determineRoutingType(EdxlMessage edxlMessage) {
        String senderId = edxlMessage.getSenderID();
        String recipientId = getRecipientID(edxlMessage);

        if (senderId.startsWith(FR_HEALTH_PREFIX)) {
            if (recipientId.startsWith(FR_HEALTH_PREFIX)) {
                return RoutingType.SAMU_TO_SAMU;
            } else if (recipientId.startsWith(FR_FIRE_PREFIX)) {
                return RoutingType.SAMU_TO_CISU;
            }
        } else if (senderId.startsWith(FR_FIRE_PREFIX)) {
            if (recipientId.startsWith(FR_HEALTH_PREFIX)) {
                return RoutingType.CISU_TO_SAMU;
            }
        }
        throw new RuntimeException(
                String.format(
                        "Cannot determine routing type from %s to %s", senderId, recipientId));
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
