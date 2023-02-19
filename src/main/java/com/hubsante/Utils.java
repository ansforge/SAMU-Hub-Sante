package com.hubsante;

import com.rabbitmq.client.ConnectionFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;

public class Utils {
    static String getClientId(String[] strings) {
        return getRouting(strings).split("[.]")[0];
    }

    static String getMessageType(String routingKey) {
        return routingKey.split("[.]")[2];
    }

    static String getRouting(String[] strings) {
        if (strings.length < 1)
            return "anonymous.info";
        return strings[0];
    }

    static String getMessage(String[] strings) {
        if (strings.length < 2)
            return "Hello World!";
        return joinStrings(strings, " ", 1);
    }

    private static String joinStrings(String[] strings, String delimiter, int startIndex) {
        int length = strings.length;
        if (length == 0) return "";
        if (length < startIndex) return "";
        StringBuilder words = new StringBuilder(strings[startIndex]);
        for (int i = startIndex + 1; i < length; i++) {
            words.append(delimiter).append(strings[i]);
        }
        return words.toString();
    }

    static class TLS {
        public static void enableTLS(
                ConnectionFactory factory,
                String keyPassphrase,
                String keyPath,
                String trustPassphrase,
                String trustStorePath
        ) throws Exception {
            KeyManagerFactory kmf = loadClientKey(keyPassphrase.toCharArray(), keyPath);
            TrustManagerFactory tmf = loadTrustStore(trustPassphrase.toCharArray(), trustStorePath);
            SSLContext c = SSLContext.getInstance("TLSv1.2");
            c.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            factory.setHost("localhost");
            factory.setPort(5671);
            factory.useSslProtocol(c);
            factory.enableHostnameVerification();
        }

        public static KeyManagerFactory loadClientKey(char[] keyPassphrase, String keyPath) throws Exception {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(new FileInputStream(keyPath), keyPassphrase);

            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, keyPassphrase);
            return kmf;
        }

        public static TrustManagerFactory loadTrustStore(char[] trustPassphrase, String trustStorePath) throws Exception {
            KeyStore tks = KeyStore.getInstance("JKS");
            tks.load(new FileInputStream(trustStorePath), trustPassphrase);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(tks);
            return tmf;
        }
    }
}
