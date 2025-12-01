package loadtesting.simulations;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import loadtesting.AmqpConnectionFactory;
import loadtesting.SimulationUtils;
import org.galaxio.gatling.amqp.javaapi.protocol.AmqpProtocolBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static io.gatling.javaapi.core.CoreDsl.constantUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static loadtesting.Constants.JSON_CONTENT_TYPE;
import static org.galaxio.gatling.amqp.javaapi.AmqpDsl.amqp;

public class SamuNexsisSimulation extends Simulation {
    private final static int defaultDuration = 600; // 10 minutes
    private final static int defaultUserCount = 6;
    private final static String vhost = "15-nexsis_v1.9";
    private final static String senderId = "fr.health.test.samuRC";
    private final static String recipientId = "fr.fire.nexsis.sdisZ";

    private final Logger log = LoggerFactory.getLogger(getClass());

    {
        try {
            AmqpConnectionFactory connectionFactory = new AmqpConnectionFactory();
            AmqpProtocolBuilder connection = connectionFactory.buildAmqpProtocolBuilder(vhost);

            String fileContent = SimulationUtils.loadSampleFile("rc-eda.json");

            Iterator<Map<String, Object>> messageFeeder = Stream.generate((Supplier<Map<String, Object>>) () ->
            {
                try {
                    return Collections.singletonMap("message", SimulationUtils.buildEdxlMessageString(fileContent, senderId, recipientId));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).iterator();

            ScenarioBuilder scenario = scenario("15-18: direct transfer")
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
            int userCount = SimulationUtils.getNumericEnvVar("SAMU_NEXSIS_SCENARIO_USER_COUNT", defaultUserCount);

            setUp(
                    scenario.injectOpen(
                            constantUsersPerSec(userCount).during(duration)
                    ).protocols(connection)
            ).maxDuration(duration * 2L);
        } catch (Exception e) {
            log.error("Unexpected error during load test", e);
        }
    }
}