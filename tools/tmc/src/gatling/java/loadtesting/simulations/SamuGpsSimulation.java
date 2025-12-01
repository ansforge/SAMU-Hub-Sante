package loadtesting.simulations;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import loadtesting.AmqpConnectionFactory;
import loadtesting.SimulationUtils;
import org.galaxio.gatling.amqp.javaapi.protocol.AmqpProtocolBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.gatling.javaapi.core.CoreDsl.constantUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.scenario;

public class SamuGpsSimulation extends Simulation {
    private final static int defaultDuration = 600; // 10 minutes
    private final static int defaultUserCount = 160;
    private final static String vhost = "15-gps_v1.3";

    private final Logger log = LoggerFactory.getLogger(getClass());

    {
        try {
            String messageString = SimulationUtils.loadSampleMessage("geo-position.json");

            AmqpConnectionFactory connectionFactory = new AmqpConnectionFactory();
            AmqpProtocolBuilder gpsConnection = connectionFactory.buildAmqpProtocolBuilder(vhost);

            ScenarioBuilder gpsScenario = scenario("GEO-POS").exec(SimulationUtils.sendAmqpMessage("fr.health.test.samuA", messageString));

            int duration = SimulationUtils.getNumericEnvVar("SCENARIO_DURATION", defaultDuration);
            int userCount = SimulationUtils.getNumericEnvVar("SAMU_GPS_SCENARIO_USER_COUNT", defaultUserCount);

            setUp(
                    gpsScenario.injectOpen(
                            constantUsersPerSec(userCount).during(duration)
                    ).protocols(gpsConnection)
            ).maxDuration(duration * 2L);
        } catch (Exception e) {
            log.error("Unexpected error during load test", e);
        }
    }
}