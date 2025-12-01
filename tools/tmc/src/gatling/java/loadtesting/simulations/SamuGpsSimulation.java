package loadtesting.simulations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubsante.model.Utils;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import loadtesting.AmqpConnectionFactory;
import loadtesting.SimulationUtils;
import org.galaxio.gatling.amqp.javaapi.protocol.AmqpProtocolBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

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

    {
        try {
            AmqpConnectionFactory connectionFactory = new AmqpConnectionFactory();
            AmqpProtocolBuilder gpsConnection = connectionFactory.buildAmqpProtocolBuilder(vhost);

            String fileContent = SimulationUtils.loadSampleFile("geo-position.json");

            Iterator<Map<String, Object>> messageFeeder = Stream.generate((Supplier<Map<String, Object>>) () ->
            {
                try {
                    return Collections.singletonMap("message", SimulationUtils.buildEdxlMessageString(fileContent, senderId, recipientId));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).iterator();

            ScenarioBuilder gpsScenario = scenario("GEO-POS")
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