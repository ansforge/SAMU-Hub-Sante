package tnr;

import java.util.Arrays;
import java.util.UUID;

import com.fasterxml.jackson.databind.node.ObjectNode;

import tnr.dto.MessageDTO;

public class Utils {

    public static String sanitizeClientId(String clientId) {
        if (clientId == null) {
            return null;
        }
        String[] parts = clientId.split("\\.");
        if (parts.length <= 2) {
            return "";
        }
        return String.join(".", Arrays.copyOfRange(parts, 2, parts.length));
    }

    public static boolean isMessageOfType(MessageDTO message, String type) {
        return message.getPayload()
                .path("content").path(0)
                .path("jsonContent")
                .path("embeddedJsonContent")
                .path("message")
                .has(type);
    }

    public static String getReferencedDistributionID(ObjectNode payload) {
        return payload
                .path("content").path(0)
                .path("jsonContent")
                .path("embeddedJsonContent")
                .path("message")
                .path("reference")
                .path("distributionID")
                .asText(null);
    }

    public static String generateDistributionId(String clientId) {
        return clientId + "_" + UUID.randomUUID();
    }
}
