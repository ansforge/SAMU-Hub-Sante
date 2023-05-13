package com.hubsante;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Utils {

    /*
    * Dans le contexte de ce code exemple, cette méthode permet de récupérer un clientId à partir
    * de la routing key fournie en argument de run
     */
    static String getClientId(String[] strings) {
        String[] routing = getRouting(strings).split("[.]");
        List<String> routingKey = Arrays.stream(routing).limit(routing.length-2).collect(Collectors.toList());
        return String.join(".", routingKey);
    }

    /*
    * Dans le contexte de ce code exemple, cette méthode permet de récupérer la routing key
    * passée en argument de run
     */
    static String getRouting(String[] strings) {
        if (strings.length < 1)
            return "anonymous.info";
        return strings[0];
    }

    static boolean isJsonScheme(String scheme) {
        return scheme.equalsIgnoreCase("json");
    }
}
