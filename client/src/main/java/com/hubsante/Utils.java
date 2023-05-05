package com.hubsante;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubsante.message.*;
import com.rabbitmq.client.ConnectionFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.security.KeyStore;
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

    static String getClientSuffix(String clientId) {
        String[] keys = clientId.split("[.]");
        return keys[keys.length -1];
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
}
