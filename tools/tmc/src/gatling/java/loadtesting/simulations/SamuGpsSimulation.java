package loadtesting.simulations;

import io.gatling.javaapi.core.PopulationBuilder;
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
import static loadtesting.Constants.JSON_CONTENT_TYPE;
import static org.galaxio.gatling.amqp.javaapi.AmqpDsl.amqp;

public class SamuGpsSimulation extends Simulation {
    private final static int defaultDuration = 600; // 10 minutes
    private final static int defaultUserCount = 160;
    private final static String vhost = "15-gps_v1.3";
    private final static String senderId = "fr.health.test.samuA";
    private final static String recipientId = "fr.health.test.samuC";

    private final Logger log = LoggerFactory.getLogger(getClass());

    public static ScenarioBuilder getScenario() throws Exception {
        String fileContent = SimulationUtils.loadSampleFile("geo-position.json");
        Iterator<Map<String, Object>> messageFeeder = SimulationUtils.generateMessageFeeder(fileContent, senderId, recipientId);
        return scenario("15-GSP: geo position update")
                .feed(
                        messageFeeder
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
        return SimulationUtils.getNumericEnvVar("SAMU_GPS_SCENARIO_USER_COUNT", defaultUserCount);
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
        } catch (Exception e) {
            log.error("Unexpected error during load test", e);
        }
    }
}