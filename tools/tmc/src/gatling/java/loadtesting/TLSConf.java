package loadtesting;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;

public class TLSConf {

    private SSLContext sslContext;
    private static final String CERTIFICATE_KEYSTORE_INSTANCE_NAME = "PKCS12";
    private static final String TRUST_STORE_KEYSTORE_INSTANCE_NAME = "JKS";
    private static final String KEY_MANAGER_FACTORY_NAME = "SunX509";

    public TLSConf(String protocol, String keyPassphrase, String keyPath, String trustPassphrase, String trustStorePath) throws Exception {
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
        KeyStore ks = KeyStore.getInstance(CERTIFICATE_KEYSTORE_INSTANCE_NAME);
        ks.load(new FileInputStream(keyPath), keyPassphrase);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KEY_MANAGER_FACTORY_NAME);
        kmf.init(ks, keyPassphrase);
        return kmf;
    }

    public static TrustManagerFactory loadTrustStore(char[] trustPassphrase, String trustStorePath) throws Exception {
        KeyStore tks = KeyStore.getInstance(TRUST_STORE_KEYSTORE_INSTANCE_NAME);
        tks.load(new FileInputStream(trustStorePath), trustPassphrase);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(KEY_MANAGER_FACTORY_NAME);
        tmf.init(tks);
        return tmf;
    }
}
