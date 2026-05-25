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

    public static ConversionParametersDTO determineConversionParameters(
            HubConfiguration hubConfig, EdxlMessage edxlMessage) {
        RoutingType routingType = determineRoutingType(edxlMessage);

        String recipientId = getRecipientID(edxlMessage);
        String currentVhost = hubConfig.getVhost();

        switch (routingType) {
            case SAMU_TO_SAMU:
                String[] targetVersionsOnHealthPerimeter =
                        hubConfig.getClientVersionsForPerimeter(
                                recipientId, Perimeter.HEALTH.getName());
                String[] availableHealthVhosts =
                        formatPerimeterVersionListToVhosts(
                                targetVersionsOnHealthPerimeter, Perimeter.HEALTH.getName());

                if (availableHealthVhosts == null) {
                    return null;
                }

                boolean isConversionNeeded =
                        !Arrays.asList(availableHealthVhosts).contains(currentVhost);

                if (!isConversionNeeded) {
                    return null;
                }

                return new ConversionParametersDTO(
                        edxlMessage,
                        getVHostMatchingModelVersion(currentVhost),
                        getVHostMatchingModelVersion(
                                availableHealthVhosts[availableHealthVhosts.length - 1]),
                        availableHealthVhosts[availableHealthVhosts.length - 1],
                        ConversionType.HEALTH_VERSION_CONVERSION);
            case CISU_TO_SAMU:
                String[] targetVersionsOnCISUPerimeter =
                        hubConfig.getClientVersionsForPerimeter(
                                recipientId, Perimeter.CISU.getName());

                if (targetVersionsOnCISUPerimeter == null
                        || targetVersionsOnCISUPerimeter.length == 0) {
                    String[] targetVersionsOnHealthPerimeter2 =
                            hubConfig.getClientVersionsForPerimeter(
                                    recipientId, Perimeter.HEALTH.getName());
                    String[] availableHealthVhosts2 =
                            formatPerimeterVersionListToVhosts(
                                    targetVersionsOnHealthPerimeter2, Perimeter.HEALTH.getName());

                    if (availableHealthVhosts2 == null) {
                        return null;
                    }

                    boolean isConversionNeeded2 =
                            !Arrays.asList(availableHealthVhosts2).contains(currentVhost);

                    if (!isConversionNeeded2) {
                        return null;
                    }

                    return new ConversionParametersDTO(
                            edxlMessage,
                            getVHostMatchingModelVersion(currentVhost),
                            getVHostMatchingModelVersion(
                                    availableHealthVhosts2[availableHealthVhosts2.length - 1]),
                            availableHealthVhosts2[availableHealthVhosts2.length - 1],
                            ConversionType.CISU_TRANSCODING);
                }

                String[] availableCISUVhosts =
                        formatPerimeterVersionListToVhosts(
                                targetVersionsOnCISUPerimeter, Perimeter.CISU.getName());

                if (availableCISUVhosts == null) {
                    return null;
                }

                boolean isConversionNeeded2 =
                        !Arrays.asList(availableCISUVhosts).contains(currentVhost);

                if (!isConversionNeeded2) {
                    return null;
                }

                return new ConversionParametersDTO(
                        edxlMessage,
                        getVHostMatchingModelVersion(currentVhost),
                        getVHostMatchingModelVersion(
                                availableCISUVhosts[availableCISUVhosts.length - 1]),
                        availableCISUVhosts[availableCISUVhosts.length - 1],
                        ConversionType.CISU_VERSION_CONVERSION);
            case SAMU_TO_CISU:
                if (currentVhost.startsWith(Perimeter.HEALTH.getName())) {
                    return new ConversionParametersDTO(
                            edxlMessage,
                            getVHostMatchingModelVersion(currentVhost),
                            getVHostMatchingModelVersion(NEXSIS_VHOST),
                            NEXSIS_VHOST,
                            ConversionType.CISU_TRANSCODING);
                }
                if (currentVhost.startsWith(Perimeter.CISU.getName())) {
                    if (currentVhost.equals(NEXSIS_VHOST)) {
                        return null;
                    }
                    return new ConversionParametersDTO(
                            edxlMessage,
                            getVHostMatchingModelVersion(currentVhost),
                            getVHostMatchingModelVersion(NEXSIS_VHOST),
                            NEXSIS_VHOST,
                            ConversionType.CISU_VERSION_CONVERSION);
                }
                throw new RuntimeException("OF THE FUCK DID THIS MESSAGE ENDED UP HERE ?????");
            default:
                throw new RuntimeException("WTF IS THIS CONVERSION TYPE ?????");
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
