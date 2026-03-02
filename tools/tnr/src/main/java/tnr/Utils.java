package tnr;

import java.util.Arrays;

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
}
