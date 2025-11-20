package example;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultSaslConfig;
import com.rabbitmq.client.RecoveryDelayHandler;

public class MTLSConnectionFactory {
    private final String host;
    private final int port;

    public MTLSConnectionFactory(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public ConnectionFactory buildConnectionToVhost(TLSConf tlsConf, String vhost) throws Exception {
        final ConnectionFactory factory = new ConnectionFactory();

        factory.setSaslConfig(DefaultSaslConfig.EXTERNAL);
        factory.setHost(this.host);
        factory.setPort(this.port);
        factory.setVirtualHost(vhost);

        // Here, configure the connection recovery policies
        // NB - You can set a fixed time interval using setNetworkRecoveryInterval(NETWORK_RECOVERY_INTERVAL);
        // NB - You can optionally configure ExponentialBackoffDelayHandler with your own backoff sequence.
        factory.setAutomaticRecoveryEnabled(true);
        final RecoveryDelayHandler delayHandler = new RecoveryDelayHandler.ExponentialBackoffDelayHandler();
        factory.setRecoveryDelayHandler(delayHandler);

        factory.useSslProtocol(tlsConf.getSslContext());

        factory.enableHostnameVerification();

        return factory;
    }
}
