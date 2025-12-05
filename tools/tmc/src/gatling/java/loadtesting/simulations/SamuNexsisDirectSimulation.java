package loadtesting.simulations;

import io.gatling.javaapi.core.PopulationBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import loadtesting.AmqpConnectionFactory;
import loadtesting.SimulationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import static io.gatling.javaapi.core.CoreDsl.constantUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static loadtesting.Constants.JSON_CONTENT_TYPE;
import static org.galaxio.gatling.amqp.javaapi.AmqpDsl.amqp;

public class SamuNexsisDirectSimulation extends Simulation {
    private final static int defaultDuration = 600; // 10 minutes
    private final static int defaultUserCount = 3;
    private final static String vhost = "15-nexsis_v1.9";
    private final static String senderId = "fr.health.test.samuRC";
    private final static String recipientId = "fr.fire.nexsis.sdisZ";

    private final Logger log = LoggerFactory.getLogger(getClass());

    public static ScenarioBuilder getScenario() throws Exception {
        String createCaseContent = SimulationUtils.loadSampleFile("rc-eda.json");
        Iterator<Map<String, Object>> directScenarioFeeder = SimulationUtils.generateMessageFeeder(createCaseContent, senderId, recipientId);
        return scenario("15-18: direct transfer")
                .feed(
                        directScenarioFeeder
                )
                .exec(
                        amqp("publish message as client " + senderId)
                                .publish()
                                .topicExchange("hubsante", senderId)
                                .textMessage("#{message}")
                                .contentType(JSON_CONTENT_TYPE)
                );
    }

    public static int getUserCount() {
        return SimulationUtils.getNumericEnvVar("SAMU_NEXSIS_DIRECT_SCENARIO_USER_COUNT", defaultUserCount);
    }

    public static PopulationBuilder setupScenarioPopulation(int duration, AmqpConnectionFactory connectionFactory) throws Exception {
        return getScenario().injectOpen(
                constantUsersPerSec(getUserCount()).during(duration)
        ).protocols(connectionFactory.buildAmqpProtocolBuilder(vhost));
    }

    {
        try {
            AmqpConnectionFactory connectionFactory = new AmqpConnectionFactory();
            int duration = SimulationUtils.getNumericEnvVar("SCENARIO_DURATION", defaultDuration);

            setUp(
                    setupScenarioPopulation(duration, connectionFactory)
            ).maxDuration(duration * 2L);
        } catch (
                Exception e) {
            log.

                    error("Unexpected error during load test", e);
        }
    }
}