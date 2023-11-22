/**
 * Copyright © 2023 Agence du Numerique en Sante (ANS)
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
package com.hubsante.hub.service.utils;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;

public class SSLTestUtils {

    private static final String TRUST_STORE_PATH = Thread.currentThread().getContextClassLoader()
            .getResource("config/certs/trustStore").getPath();
    private static final String TRUST_STORE_PASSPHRASE = "trustStore";

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

    public static SSLContext getSSlContext(String p12Path, String p12Passphrase) throws Exception {

        KeyManagerFactory kmf = loadClientKey(p12Passphrase.toCharArray(), p12Path);
        TrustManagerFactory tmf = loadTrustStore(TRUST_STORE_PASSPHRASE.toCharArray(), TRUST_STORE_PATH);
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return sslContext;
    }
}
