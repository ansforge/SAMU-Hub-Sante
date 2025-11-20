package example;

import io.gatling.javaapi.core.PopulationBuilder;
import org.galaxio.gatling.amqp.javaapi.protocol.AmqpProtocolBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;

import static example.Constants.JSON_CONTENT_TYPE;
import static io.gatling.javaapi.core.CoreDsl.*;
import static org.galaxio.gatling.amqp.javaapi.AmqpDsl.*;
import static example.Constants.TLS_PROTOCOL_VERSION;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import io.github.cdimascio.dotenv.Dotenv;

public class PublishExample extends Simulation {
    private static final String EXCHANGE_NAME_ENV_VAR = "EXCHANGE_NAME";
    private static final String RABBITMQ_HOST_ENV_VAR = "RABBITMQ_HOST";
    private static final String RABBITMQ_PORT_ENV_VAR = "RABBITMQ_PORT";
    private static final String KEY_PASSPHRASE_ENV_VAR = "KEY_PASSPHRASE";
    private static final String CERTIFICATE_PATH_ENV_VAR = "CERTIFICATE_PATH";
    private static final String TRUST_STORE_PASSWORD_ENV_VAR = "TRUST_STORE_PASSWORD";
    private static final String TRUST_STORE_PATH_ENV_VAR = "TRUST_STORE_PATH";

    private static final Dotenv dotenv = Dotenv.load();
    private static final Logger log = LoggerFactory.getLogger(PublishExample.class);
    private static final String EXCHANGE_NAME = dotenv.get(EXCHANGE_NAME_ENV_VAR);
    private static MTLSConnectionFactory mtlsConnectionFactory;
    private static TLSConf tlsConf;

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
                                    .topicExchange(EXCHANGE_NAME, clientId)
                                    .textMessage(messageString)
                                    .contentType(JSON_CONTENT_TYPE)
                    );
    }

    private AmqpProtocolBuilder amqpConfFactory(String vhost) throws Exception {
        return amqp()
                .connectionFactory(
                        mtlsConnectionFactory.buildConnectionToVhost(tlsConf, vhost)
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
            String host = dotenv.get(RABBITMQ_HOST_ENV_VAR);
            int port = Integer.parseInt(dotenv.get(RABBITMQ_PORT_ENV_VAR));
            mtlsConnectionFactory = new MTLSConnectionFactory(host, port);
            tlsConf = new TLSConf(
                    TLS_PROTOCOL_VERSION,
                    dotenv.get(KEY_PASSPHRASE_ENV_VAR),
                    dotenv.get(CERTIFICATE_PATH_ENV_VAR),
                    dotenv.get(TRUST_STORE_PASSWORD_ENV_VAR),
                    dotenv.get(TRUST_STORE_PATH_ENV_VAR));

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
            log.error("Unexpected error during load test", e);
        }
    }
}