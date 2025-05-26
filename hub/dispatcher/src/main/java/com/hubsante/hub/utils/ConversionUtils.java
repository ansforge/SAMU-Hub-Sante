/**
 * Copyright © 2023-2025 Agence du Numerique en Sante (ANS)
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

import lombok.extern.slf4j.Slf4j;

import com.hubsante.model.cisu.CreateCaseWrapper;
import com.hubsante.model.health.CreateCaseHealthWrapper;
import com.hubsante.model.edxl.EdxlMessage;
import com.hubsante.hub.config.HubConfiguration;

import static com.hubsante.hub.config.AmqpConfiguration.TRANSFER_EXCHANGE_PREFIX;
import static com.hubsante.hub.config.Constants.*;
import static com.hubsante.hub.utils.MessageUtils.*;

import java.util.Arrays;

@Slf4j
public class ConversionUtils {

    private final static boolean DEFAULT_DIRECT_CISU_PREFERENCE = false;

    public static String buildExchangeDestination(String sourceVHost, String targetVHost) {
        return TRANSFER_EXCHANGE_PREFIX + sourceVHost + "_to_" + targetVHost;
    }

    public static boolean requiresConversion(HubConfiguration hubConfig, EdxlMessage edxlMessage) {
        boolean isCisuConversion = requiresCisuConversion(hubConfig, edxlMessage);
        boolean isVersionConversion = requiresVersionConversion(hubConfig, edxlMessage);

        return isVersionConversion || isCisuConversion;
    }

    public static boolean requiresVersionConversion(HubConfiguration hubConfig, EdxlMessage edxlMessage) {
        String sourceVHost = getSourceVHost(hubConfig);
        String[] targetVHosts = getTargetVHosts(hubConfig, edxlMessage);

        if (hubConfig.getDirectCisuPreferences().get(edxlMessage.getSenderID()) != null && hubConfig.getDirectCisuPreferences().get(edxlMessage.getSenderID())) {
            return false;
        }
        if (targetVHosts == null || sourceVHost == null || targetVHosts.length == 0) {
            return false;
        }
        if (VHOST_MODEL_VERSION.get(sourceVHost) == null) {
            return false;
        }
        return !Arrays.asList(targetVHosts).contains(sourceVHost);
    }

    public static String getSourceVHost(HubConfiguration hubConfig) {
        return hubConfig.getVhost();
    }

    public static String[] getTargetVHosts(HubConfiguration hubConfig, EdxlMessage edxlMessage) {
        String recipientID = getRecipientID(edxlMessage);
        String[] targetVHosts = hubConfig.getLrmPerimeterVersions().get(recipientID);

        if (targetVHosts == null && (recipientID.startsWith(FR_FIRE_PREFIX) || recipientID.startsWith(FR_CISU_PREFIX))) {
            targetVHosts = new String[]{NEXSIS_VHOST};
        }
        else if (targetVHosts != null) {
            targetVHosts = Arrays.stream(targetVHosts).map(version -> HEALTH_VHOST_PREFIX + version).toArray(String[]::new);
        }

        return targetVHosts;
    }

    public static boolean requiresCisuConversion(HubConfiguration hubConfig, EdxlMessage edxlMessage) {
        return isCisuExchange(edxlMessage)
                && isConvertedModel(edxlMessage)
                && !isAlreadyCisuConverted(hubConfig.getVhost(), edxlMessage.getDescriptor().getExplicitAddress().getExplicitAddressValue())
                && !isDirectCisuForHealthActor(hubConfig, edxlMessage);
    }

    public static boolean isAlreadyCisuConverted(String currentVHost, String recipient) {
        if (recipient.startsWith(FR_HEALTH_PREFIX)) {
            return currentVHost.startsWith(HEALTH_VHOST_PREFIX);
        } else {
            return currentVHost.startsWith(NEXSIS_VHOST);
        }
    }

    public static boolean isCisuExchange(EdxlMessage edxlMessage) {
        // Checks if the message is from or to CISU (not health)
        String recipientID = getRecipientID(edxlMessage);
        String senderID = edxlMessage.getSenderID();
        return !(recipientID.startsWith(HEALTH_PREFIX) && senderID.startsWith(HEALTH_PREFIX));
    }

    public static boolean isConvertedModel(EdxlMessage edxlMessage) {
        // Checks if the message is a CISU model
        // ToDo: Remove if not used (nor adapted to only directCisuModel to target only EDA and not EMSI)
        //  OR add a class in model lib to know if the message is a CISU model (to decouple dispatcher from model lib)
        return edxlMessage.getFirstContentMessage() instanceof CreateCaseWrapper
                || edxlMessage.getFirstContentMessage() instanceof CreateCaseHealthWrapper;
    }

    public static boolean isDirectCisuForHealthActor(HubConfiguration hubConfig, EdxlMessage edxlMessage) {
        // Checks if the health actor is direct CISU
        String recipientID = getRecipientID(edxlMessage);
        String senderID = edxlMessage.getSenderID();
        String healthActor = senderID.startsWith(HEALTH_PREFIX) ? senderID : recipientID;
        Boolean directCisuPreference = hubConfig.getDirectCisuPreferences().getOrDefault(healthActor, DEFAULT_DIRECT_CISU_PREFERENCE);
        return directCisuPreference != null && directCisuPreference;
    }
}
