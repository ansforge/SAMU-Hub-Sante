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

import com.hubsante.model.cisu.CreateCaseWrapper;
import com.hubsante.model.health.CreateCaseHealthWrapper;
import com.hubsante.model.edxl.EdxlMessage;
import com.hubsante.hub.config.HubConfiguration;

import static com.hubsante.hub.config.AmqpConfiguration.TRANSFER_EXCHANGE_PREFIX;
import static com.hubsante.hub.utils.MessageUtils.HEALTH_PREFIX;
import static com.hubsante.hub.utils.MessageUtils.getRecipientID;

public class ConversionUtils {
    private final static boolean DEFAULT_DIRECT_CISU_PREFERENCE = false;

    public static String buildExchangeDestination(String sourceVersion, String targetVersion) {
        return TRANSFER_EXCHANGE_PREFIX + sourceVersion.toUpperCase() + "to" + targetVersion.toUpperCase();
    }

    public static boolean requiresConversion(HubConfiguration hubConfig, EdxlMessage edxlMessage){
        boolean isCisuConversion = requiresCisuConversion(hubConfig, edxlMessage);
        boolean isVersionConversion = requiresVersionConversion(hubConfig, edxlMessage);

        return isVersionConversion || isCisuConversion;
    }

    public static boolean requiresVersionConversion(HubConfiguration hubConfig, EdxlMessage edxlMessage){
        String recipientID = getRecipientID(edxlMessage);
        String senderID = edxlMessage.getSenderID();

        String[] sourceVersions= hubConfig.getLrmPerimeterVersions().get(senderID);
        String[] targetVersions= hubConfig.getLrmPerimeterVersions().get(recipientID);

        if (sourceVersions == null || targetVersions == null){
            return false;
        }

        if (sourceVersions.length != 1 || targetVersions.length != 1){
            return false;
        }

        return sourceVersions[0] != targetVersions[0];
    }

    public static String getSourceVersion(HubConfiguration hubConfig, EdxlMessage edxlMessage){
        String senderID = edxlMessage.getSenderID();

        String[] sourceVersions= hubConfig.getLrmPerimeterVersions().get(senderID);

        if (sourceVersions == null || sourceVersions.length == 0){ return null;}

        return sourceVersions[0]; // todo - choix arbitraire, à changer quand la refacto des versions sera faite (passer de list[] à une version par client)
    }

    public static String getTargetVersion(HubConfiguration hubConfig, EdxlMessage edxlMessage){
        String recipientID = getRecipientID(edxlMessage);

        String[] targetVersions= hubConfig.getLrmPerimeterVersions().get(recipientID);

        if (targetVersions == null || targetVersions.length == 0){ return null;}

        return targetVersions[0]; // todo - choix arbitraire, à changer quand la refacto des versions sera faite (passer de list[] à une version par client)
    }

    public static boolean requiresCisuConversion(HubConfiguration hubConfig, EdxlMessage edxlMessage) {
        return isCisuExchange(edxlMessage) &&
               isConvertedModel(edxlMessage) &&
               !isDirectCisuForHealthActor(hubConfig, edxlMessage);
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
