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

    public static String buildExchangeDestination(String sourceVHost, String targetVHost) {
        return TRANSFER_EXCHANGE_PREFIX + sourceVHost + "_to_" + targetVHost;
    }

    public static boolean requiresConversion(HubConfiguration hubConfig, EdxlMessage edxlMessage) {
        boolean isCisuConversion = requiresCisuConversion(hubConfig, edxlMessage);
        boolean isVersionConversion = requiresVersionConversion(hubConfig, edxlMessage);

        return isVersionConversion || isCisuConversion;
    }

    public static boolean requiresVersionConversion(
            HubConfiguration hubConfig, EdxlMessage edxlMessage) {
        String sourceVHost = getSourceVHost(hubConfig);
        String[] targetVHosts = getTargetVHosts(hubConfig, edxlMessage);

        if (targetVHosts == null || sourceVHost == null || targetVHosts.length == 0) {
            return false;
        }
        if (!isConversionAvailable(sourceVHost)) {
            return false;
        }

        return !Arrays.asList(targetVHosts).contains(sourceVHost);
    }

    public static boolean isConversionAvailable(String vhost) {
        return CONVERSION_VHOST_MODEL.get(vhost) != null;
    }

    public static String getSourceVHost(HubConfiguration hubConfig) {
        return hubConfig.getVhost();
    }

    public static String[] getTargetVHosts(HubConfiguration hubConfig, EdxlMessage edxlMessage) {
        String recipientId = getRecipientID(edxlMessage);
        String senderId = edxlMessage.getSenderID();
        String sourceVhost = hubConfig.getVhost(); // ex '15-15_v1.5'
        String sourcePerimeter = trimVersionSuffix(sourceVhost); // ex '15-15'
        String[] targetVersionsOnTargetPerimeter = new String[] {};

        // CISU conversion case - recipient and sender are on different vhosts
        boolean isNexsisRecipient =
                recipientId.startsWith(FR_FIRE_PREFIX) || recipientId.startsWith(FR_CISU_PREFIX);
        if (isNexsisRecipient) {
            return new String[] {NEXSIS_VHOST}; // ["15-nexsis_v1.9"]
        }
        boolean isCisuSender = !senderId.startsWith(FR_HEALTH_PREFIX);
        boolean isDirectCisu = isDirectCisuForHealthActor(hubConfig, edxlMessage);

        String targetPerimeter =
                (isCisuSender && !isDirectCisu) ? Perimeter.HEALTH.getName() : sourcePerimeter;

        targetVersionsOnTargetPerimeter =
                hubConfig.getClientVersionsForPerimeter(
                        recipientId, targetPerimeter); // ex ['1.5, 2.0']
        return formatVersionToVhosts(
                targetVersionsOnTargetPerimeter,
                targetPerimeter); // ex ["15-15_v1.5", "15-15_v2.0"]
    }

    public static String[] formatVersionToVhosts(String[] versions, String sourcePerimeter) {
        if (versions != null) {
            return Arrays.stream(versions)
                    .map(version -> sourcePerimeter + "_v" + version)
                    .toArray(String[]::new);
        }
        return null;
    }

    public static boolean requiresCisuConversion(
            HubConfiguration hubConfig, EdxlMessage edxlMessage) {
        return isOneCisuHubexInvolved(edxlMessage)
                && !isAlreadyCisuConverted(
                        hubConfig.getVhost(),
                        edxlMessage.getDescriptor().getExplicitAddress().getExplicitAddressValue())
                && !isDirectCisuForHealthActor(hubConfig, edxlMessage);
    }

    public static String trimVersionSuffix(String input) {
        String VERSION_SUFFIX_REGEX = "_v[\\d\\.]+$";

        if (input == null) return null;
        return input.replaceFirst(VERSION_SUFFIX_REGEX, "");
    }

    public static boolean isAlreadyCisuConverted(String currentVHost, String recipient) {
        if (recipient.startsWith(FR_HEALTH_PREFIX)) {
            return currentVHost.startsWith(HEALTH_VHOST_PREFIX);
        } else {
            return currentVHost.startsWith(NEXSIS_VHOST);
        }
    }

    public static boolean isOneCisuHubexInvolved(EdxlMessage edxlMessage) {
        String recipientId = getRecipientID(edxlMessage);
        String senderId = edxlMessage.getSenderID();
        return !(recipientId.startsWith(HEALTH_PREFIX) && senderId.startsWith(HEALTH_PREFIX));
    }

    public static boolean isDirectCisuForHealthActor(
            HubConfiguration hubConfig, EdxlMessage edxlMessage) {
        // Checks if the health actor is direct CISU
        String recipientId = getRecipientID(edxlMessage);
        String senderId = edxlMessage.getSenderID();
        String healthActor = senderId.startsWith(HEALTH_PREFIX) ? senderId : recipientId;
        Boolean directCisuPreference =
                hubConfig
                        .getDirectCisuPreferences()
                        .getOrDefault(healthActor, DEFAULT_DIRECT_CISU_PREFERENCE);
        return directCisuPreference != null && directCisuPreference;
    }
}
