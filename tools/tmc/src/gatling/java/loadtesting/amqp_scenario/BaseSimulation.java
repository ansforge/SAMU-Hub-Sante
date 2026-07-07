package loadtesting.amqp_scenario;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import loadtesting.AmqpConnectionFactory;
import loadtesting.SimulationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;

import static io.gatling.javaapi.core.CoreDsl.constantUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static loadtesting.ConfigUtils.getNumericEnvVar;
import static loadtesting.Constants.*;
import static org.galaxio.gatling.amqp.javaapi.AmqpDsl.amqp;

public abstract class BaseSimulation extends Simulation {
    private final static int defaultDuration = 10;
    private final static int defaultUserCount = 2;

    private final Logger log = LoggerFactory.getLogger(getClass());
    protected abstract ScenarioConfig getConfig();

    {
        try {
            ScenarioConfig config = getConfig();

            String fileContent = SimulationUtils.loadSampleFile(config.messageSampleName());

            Iterator<Map<String, Object>> messageFeeder =
                    SimulationUtils.generateMessageFeeder(fileContent, config.senderId(), config.recipientId());

            ScenarioBuilder scenario = scenario(config.title())
                    .feed(
                            messageFeeder
                    )
                    .exec(
                            amqp(String.format("%s %s", AMQP_REQUEST_NAME, config.senderId()))
                                    .publish()
                                    .topicExchange(HUBSANTE_EXCHANGE, config.senderId())
                                    .textMessage(String.format("#{%s}", GATLING_EL_MESSAGE_KEY))
                                    .contentType(JSON_CONTENT_TYPE)
                    );

            AmqpConnectionFactory connectionFactory = new AmqpConnectionFactory();

            int duration = getNumericEnvVar("SCENARIO_DURATION", defaultDuration);
            int userCount = getNumericEnvVar(config.userCountEnvVarKey(), defaultUserCount);

            setUp(
                    scenario.injectOpen(
                            constantUsersPerSec(userCount).during(duration)
                    ).protocols(connectionFactory.buildAmqpProtocolBuilder(config.vhost()))
            ).maxDuration(duration * 2L);
        } catch (Exception e) {
            log.error("Unexpected error during load test", e);
        }
    }
}