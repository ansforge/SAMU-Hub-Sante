package com.hubsante;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;

public class TLSConf {

    private SSLContext sslContext;

    public TLSConf(String protocol, String keyPassphrase, String keyPath, String trustPassphrase, String trustStorePath)
            throws Exception {
        KeyManagerFactory kmf = loadClientKey(keyPassphrase.toCharArray(), keyPath);
        TrustManagerFactory tmf = loadTrustStore(trustPassphrase.toCharArray(), trustStorePath);
        this.sslContext = SSLContext.getInstance(protocol);
        this.sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
    }

    public SSLContext getSslContext() {
        return this.sslContext;
    }

    public void setSslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
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
