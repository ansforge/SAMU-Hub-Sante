package loadtesting;

import io.gatling.javaapi.core.PopulationBuilder;
import org.galaxio.gatling.amqp.javaapi.protocol.AmqpProtocolBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;

import static loadtesting.Constants.JSON_CONTENT_TYPE;
import static io.gatling.javaapi.core.CoreDsl.*;
import static org.galaxio.gatling.amqp.javaapi.AmqpDsl.*;
import static loadtesting.Constants.TLS_PROTOCOL_VERSION;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import io.github.cdimascio.dotenv.Dotenv;

public abstract class AmqpSimulation extends Simulation {
    private static final String EXCHANGE_NAME_ENV_VAR = "EXCHANGE_NAME";
    private static final String RABBITMQ_HOST_ENV_VAR = "RABBITMQ_HOST";
    private static final String RABBITMQ_PORT_ENV_VAR = "RABBITMQ_PORT";
    private static final String KEY_PASSPHRASE_ENV_VAR = "KEY_PASSPHRASE";
    private static final String CERTIFICATE_PATH_ENV_VAR = "CERTIFICATE_PATH";
    private static final String TRUST_STORE_PASSWORD_ENV_VAR = "TRUST_STORE_PASSWORD";
    private static final String TRUST_STORE_PATH_ENV_VAR = "TRUST_STORE_PATH";

    private final Dotenv dotenv = Dotenv.load();
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final String EXCHANGE_NAME = dotenv.get(EXCHANGE_NAME_ENV_VAR);
    private AmqpConnectionFactory amqpConnectionFactory;
    private TLSConf tlsConf;

    protected String loadSampleMessage(String fileName) throws Exception {
        InputStream fileStream = AmqpSimulation.class.getClassLoader().getResourceAsStream("messages/"+fileName);
        if (fileStream == null) throw new IOException("Resource not found:" + fileName);
        return new String(fileStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    protected ScenarioBuilder buildAMPQScenario(String name, String clientId, String messageString) {
        return scenario(name)
                .exec(
                        amqp("publish to exchange")
                                .publish()
                                .topicExchange(EXCHANGE_NAME, clientId)
                                .textMessage(messageString)
                                .contentType(JSON_CONTENT_TYPE)
                );
    }

    protected AmqpProtocolBuilder amqpConfFactory(String vhost) throws Exception {
        return amqp()
                .connectionFactory(
                        amqpConnectionFactory.buildConnectionToVhost(tlsConf, vhost)
                )
                .usePersistentDeliveryMode();
    }

    protected PopulationBuilder setupScenario(ScenarioBuilder scenario, AmqpProtocolBuilder protocol, Integer maxUsers) {
        return scenario.injectOpen(
                rampUsersPerSec(1).to(maxUsers).during(60),
                constantUsersPerSec(maxUsers).during(180),
                rampUsersPerSec(maxUsers).to(1).during(60)
        ).protocols(protocol);
    }

    private void initSimulation() throws Exception {
        String host = dotenv.get(RABBITMQ_HOST_ENV_VAR);
        int port = Integer.parseInt(dotenv.get(RABBITMQ_PORT_ENV_VAR));
        amqpConnectionFactory = new AmqpConnectionFactory(host, port);
        tlsConf = new TLSConf(
                TLS_PROTOCOL_VERSION,
                dotenv.get(KEY_PASSPHRASE_ENV_VAR),
                dotenv.get(CERTIFICATE_PATH_ENV_VAR),
                dotenv.get(TRUST_STORE_PASSWORD_ENV_VAR),
                dotenv.get(TRUST_STORE_PATH_ENV_VAR));
    }

    protected abstract void setupScenarios() throws Exception;

    {
        try {
            initSimulation();
            setupScenarios();
        } catch(Exception e) {
            log.error("Unexpected error during load test", e);
        }
    }
}