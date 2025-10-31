package example;

import org.galaxio.gatling.amqp.javaapi.protocol.AmqpProtocolBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;

import static example.Constants.JSON_CONTENT_TYPE;
import static io.gatling.javaapi.core.CoreDsl.*;
import static org.galaxio.gatling.amqp.javaapi.AmqpDsl.*;
import static example.Constants.TLS_PROTOCOL_VERSION;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultSaslConfig;
import com.rabbitmq.client.RecoveryDelayHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import io.github.cdimascio.dotenv.Dotenv;

public class PublishExample extends Simulation {
        final Dotenv dotenv = Dotenv.load();
        private static final Logger log = LoggerFactory.getLogger(PublishExample.class);
        private final String host = dotenv.get("RABBITMQ_HOST");
        private final Integer port = Integer.parseInt(dotenv.get("RABBITMQ_PORT"));
        private final String exchangeName = dotenv.get("EXCHANGE_NAME");
        private final String vhost = "15-15_v1.5";

        private ConnectionFactory mtlsFactory() throws Exception {
            final ConnectionFactory factory = new ConnectionFactory();

            factory.setSaslConfig(DefaultSaslConfig.EXTERNAL);
            factory.setHost(this.host);
            factory.setPort(this.port);
            factory.setVirtualHost(this.vhost);

            // Here, configure the connection recovery policies
            // NB - You can set a fixed time interval using setNetworkRecoveryInterval(NETWORK_RECOVERY_INTERVAL);
            // NB - You can optionally configure ExponentialBackoffDelayHandler with your own backoff sequence.
            factory.setAutomaticRecoveryEnabled(true);
            final RecoveryDelayHandler delayHandler = new RecoveryDelayHandler.ExponentialBackoffDelayHandler();
            factory.setRecoveryDelayHandler(delayHandler);


            final TLSConf tlsConf = new TLSConf(
                TLS_PROTOCOL_VERSION,
                dotenv.get("KEY_PASSPHRASE"),
                dotenv.get("CERTIFICATE_PATH"),
                dotenv.get("TRUST_STORE_PASSWORD"),
                dotenv.get("TRUST_STORE_PATH"));

            factory.useSslProtocol(tlsConf.getSslContext());

            factory.enableHostnameVerification();

            return factory;
        }

    public ScenarioBuilder scn = scenario("AMQP test")
            .feed(Utils.idFeeder)
            .exec(
                    amqp("publish to exchange")
                            .publish()
                            .topicExchange(this.exchangeName, "fr.health.test.samuA")
                            .textMessage()
                            .contentType(JSON_CONTENT_TYPE)
            );


    {
        try {
            AmqpProtocolBuilder amqpConf = amqp()
                    .connectionFactory(
                            mtlsFactory()
                    )
                    .usePersistentDeliveryMode();

            setUp(
                    scn.injectOpen(
                            rampUsersPerSec(1).to(5).during(60),
                            constantUsersPerSec(5).during(300))
            )
                    .protocols(amqpConf)
                    .maxDuration(600);
        } catch(Exception e) {
            log.error("e: ", e);
        }
    }
}