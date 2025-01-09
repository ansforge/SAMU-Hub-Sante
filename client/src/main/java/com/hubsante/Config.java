package com.hubsante;
import java.util.Optional;

public class Config {
    private String exchangeName;
    private String hubHostname;
    private String vhost;
    private int hubPort;
    private String keyPassphrase;
    private String certPath;
    private String trustStorePassword;
    private String trustStorePath;

    public Config() {
        this.exchangeName = getEnvVarOrThrow("EXCHANGE_NAME");
        this.hubHostname = getEnvVarOrThrow("HUB_HOSTNAME");
        this.vhost = getEnvVarOrThrow("VHOST");
        String hubPortValue = getEnvVarOrThrow("HUB_PORT");
        this.hubPort = Integer.parseInt(hubPortValue);
        this.keyPassphrase = getEnvVarOrThrow("KEY_PASSPHRASE");
        this.certPath = getEnvVarOrThrow("CERT_PATH");
        this.trustStorePassword = getEnvVarOrThrow("TRUST_STORE_PASSWORD");
        this.trustStorePath = getEnvVarOrThrow("TRUST_STORE_PATH");
    }

    private String getEnvVarOrThrow(String variableName) {
        return Optional.ofNullable(System.getenv(variableName))
                .orElseThrow(() -> new IllegalStateException(variableName + " environment variable is not defined"));
    }

    public String getExchangeName() {
        return exchangeName;
    }

    public String getHubHostname() {
        return hubHostname;
    }

    public String getVhost() {
        return vhost;
    }

    public int getHubPort() {
        return hubPort;
    }

    public String getKeyPassphrase() {
        return keyPassphrase;
    }

    public String getCertPath() {
        return certPath;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public String getTrustStorePath() {
        return trustStorePath;
    }
}
