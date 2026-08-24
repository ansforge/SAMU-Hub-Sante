package tnr;

import java.util.Arrays;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
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

    public static JsonNode getMessageNode(MessageDTO message) {
        if (message.isXML()) {
            return message.getPayload()
                    .path("content")
                    .path("contentObject")
                    .path("contentXML")
                    .path("embeddedXMLContent")
                    .path("message");
        }
        return message.getPayload()
                .path("content").path(0)
                .path("jsonContent")
                .path("embeddedJsonContent")
                .path("message");
    }

    public static JsonNode getUseCaseNode(MessageDTO message, MessageType type) {
        return getMessageNode(message).path(type.value());
    }

    public static boolean isMessageOfType(MessageDTO message, MessageType type) {
        return getMessageNode(message).has(type.value());
    }

    public static String getReferencedDistributionID(MessageDTO message) {
        return getMessageNode(message)
                .path("reference")
                .path("distributionID")
                .asText(null);
    }

    public static String generateDistributionId(String clientId) {
        return clientId + "_" + UUID.randomUUID();
    }
}
