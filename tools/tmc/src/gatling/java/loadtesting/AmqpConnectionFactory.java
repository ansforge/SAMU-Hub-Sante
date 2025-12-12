package loadtesting;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultSaslConfig;
import com.rabbitmq.client.RecoveryDelayHandler;
import org.galaxio.gatling.amqp.javaapi.protocol.AmqpProtocolBuilder;

import static loadtesting.Constants.TLS_PROTOCOL_VERSION;
import static loadtesting.ConfigUtils.getEnvVar;
import static org.galaxio.gatling.amqp.javaapi.AmqpDsl.amqp;

public class AmqpConnectionFactory {
    private static final String RABBITMQ_HOST_ENV_VAR = "RABBITMQ_HOST";
    private static final String RABBITMQ_PORT_ENV_VAR = "RABBITMQ_PORT";
    private static final String KEY_PASSPHRASE_ENV_VAR = "KEY_PASSPHRASE";
    private static final String CERTIFICATE_PATH_ENV_VAR = "CERTIFICATE_PATH";
    private static final String TRUST_STORE_PASSWORD_ENV_VAR = "TRUST_STORE_PASSWORD";
    private static final String TRUST_STORE_PATH_ENV_VAR = "TRUST_STORE_PATH";

    private final String host;
    private final int port;
    private final TLSConf tlsConf;

    public AmqpConnectionFactory() throws Exception {
        String host = getEnvVar(RABBITMQ_HOST_ENV_VAR);
        int port = Integer.parseInt(getEnvVar(RABBITMQ_PORT_ENV_VAR));

        this.host = host;
        this.port = port;
        this.tlsConf = new TLSConf(
                TLS_PROTOCOL_VERSION,
                getEnvVar(KEY_PASSPHRASE_ENV_VAR),
                getEnvVar(CERTIFICATE_PATH_ENV_VAR),
                getEnvVar(TRUST_STORE_PASSWORD_ENV_VAR),
                getEnvVar(TRUST_STORE_PATH_ENV_VAR));
    }

    private ConnectionFactory buildConnectionFactoryToVhost(String vhost) {
        final ConnectionFactory factory = new ConnectionFactory();

        factory.setSaslConfig(DefaultSaslConfig.EXTERNAL);
        factory.setHost(host);
        factory.setPort(port);
        factory.setVirtualHost(vhost);
        factory.setAutomaticRecoveryEnabled(true);

        final RecoveryDelayHandler delayHandler = new RecoveryDelayHandler.ExponentialBackoffDelayHandler();
        factory.setRecoveryDelayHandler(delayHandler);

        factory.useSslProtocol(tlsConf.getSslContext());

        factory.enableHostnameVerification();

        return factory;
    }

    public AmqpProtocolBuilder buildAmqpProtocolBuilder(String vhost) {
        return amqp()
                .connectionFactory(
                        buildConnectionFactoryToVhost(vhost)
                )
                .usePersistentDeliveryMode();
    }
}
