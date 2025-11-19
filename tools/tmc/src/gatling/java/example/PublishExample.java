package example;

import io.gatling.javaapi.core.PopulationBuilder;
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

    private ConnectionFactory mtlsFactory(String vhost) throws Exception {
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

    private String loadSampleMessage(String fileName) throws Exception {
        InputStream fileStream = PublishExample.class.getClassLoader().getResourceAsStream("messages/"+fileName);
        if (fileStream == null) throw new IOException("Resource not found:" + fileName);
        return new String(fileStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    private ScenarioBuilder buildAMPQScenario(String name, String clientId, String messageString) {
        return scenario(name)
                    .feed(Utils.idFeeder)
                    .exec(
                            amqp("publish to exchange")
                                    .publish()
                                    .topicExchange(this.exchangeName, clientId)
                                    .textMessage(messageString)
                                    .contentType(JSON_CONTENT_TYPE)
                    );
    }

    private AmqpProtocolBuilder amqpConfFactory(String vhost) throws Exception {
        return amqp()
                .connectionFactory(
                        mtlsFactory(vhost)
                        )
                .usePersistentDeliveryMode();
    }

    private PopulationBuilder setupScenario(ScenarioBuilder scenario, AmqpProtocolBuilder protocol, Integer maxUsers) {
        return scenario.injectOpen(
                rampUsersPerSec(1).to(maxUsers).during(60),
                constantUsersPerSec(maxUsers).during(180),
                rampUsersPerSec(maxUsers).to(1).during(60)
        ).protocols(protocol);
    }

    {
        try {
            String messageString = loadSampleMessage("rs-eda.json");
            String invalidMessageString = loadSampleMessage("invalid.json");
            String conversionMessageString = loadSampleMessage("conversion.json");
            String traductionMessageString = loadSampleMessage("traduction.json");

            AmqpProtocolBuilder samuAConnection = amqpConfFactory("15-15_v1.5");
            AmqpProtocolBuilder samuv1Connection = amqpConfFactory("15-15_v1.5");
            AmqpProtocolBuilder samuv3Connection = amqpConfFactory("15-15_v2.1");
            AmqpProtocolBuilder samuBConnection = amqpConfFactory("15-nexsis_v1.9");

            ScenarioBuilder standardScenario = buildAMPQScenario("RS-EDA", "fr.health.test.samuA", messageString);
            ScenarioBuilder conversionScenario = buildAMPQScenario("Convert RS-EDA v1 to v3", "fr.health.test.samuv1", conversionMessageString);
            ScenarioBuilder translationScenario = buildAMPQScenario("Translate RS-EDA to RC-EDA", "fr.health.test.samuv3", traductionMessageString);
            ScenarioBuilder invalidMessageScenario = buildAMPQScenario("Invalid message", "fr.health.test.samuB", invalidMessageString);

            setUp(
                    setupScenario(conversionScenario, samuv1Connection, 5),
                    setupScenario(standardScenario, samuAConnection, 10),
                    setupScenario(translationScenario, samuv3Connection, 5),
                    setupScenario(invalidMessageScenario, samuBConnection, 5)
            )
                    .maxDuration(600);
        } catch(Exception e) {
            log.error("e: ", e);
        }
    }
}