package com.hubsante.hub.utils;

public class ConfigUtils {

    public static String sanitizeVhostForProm(String vhost) {
        return vhost.replace("_", "-");
    }
}
