package loadtesting.simulations;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import loadtesting.AmqpConnectionFactory;
import loadtesting.SimulationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.gatling.javaapi.core.CoreDsl.*;
import static loadtesting.ConfigUtils.getNumericEnvVar;
import static loadtesting.Constants.*;
import static org.galaxio.gatling.amqp.javaapi.AmqpDsl.amqp;

/** Load test for the RS persistence flow (15-18 direction, SAMU → NexSIS).
 * Each virtual user sends RS-RI, waits PAUSE_SECONDS, then sends RS-SR with the same caseId. */
public class SamuNexsisRsSimulation extends Simulation {

    private static final Logger log = LoggerFactory.getLogger(SamuNexsisRsSimulation.class);

    private static final String VHOST = "15-15_v2.1";
    private static final String SENDER_ID = "fr.health.test.samu1-v3";
    private static final String RECIPIENT_ID = "fr.fire.nexsis.sdisZ";
    private static final String USER_COUNT_ENV_VAR = "SAMU_NEXSIS_RS_SCENARIO_USER_COUNT";
    private static final int PAUSE_SECONDS = 5;

    {
        try {
            String rsRiContent = SimulationUtils.loadSampleFile("rs-ri.json");
            String rsSrContent = SimulationUtils.loadSampleFile("rs-sr.json");

            ScenarioBuilder rsScenario = scenario("RS flux complet — SAMU→NexSIS")
                    .feed(SimulationUtils.generateRsFlowFeeder(rsRiContent, rsSrContent, SENDER_ID, RECIPIENT_ID))
                    .exec(
                            amqp(String.format("%s %s (RS-RI)", AMQP_REQUEST_NAME, SENDER_ID))
                                    .publish()
                                    .topicExchange(HUBSANTE_EXCHANGE, SENDER_ID)
                                    .textMessage("#{rsRiMessage}")
                                    .contentType(JSON_CONTENT_TYPE)
                    )
                    .pause(PAUSE_SECONDS)
                    .exec(
                            amqp(String.format("%s %s (RS-SR)", AMQP_REQUEST_NAME, SENDER_ID))
                                    .publish()
                                    .topicExchange(HUBSANTE_EXCHANGE, SENDER_ID)
                                    .textMessage("#{rsSrMessage}")
                                    .contentType(JSON_CONTENT_TYPE)
                    );

            AmqpConnectionFactory connectionFactory = new AmqpConnectionFactory();

            int duration = getNumericEnvVar("SCENARIO_DURATION", 10);
            int userCount = getNumericEnvVar(USER_COUNT_ENV_VAR, 2);

            setUp(
                    rsScenario.injectOpen(constantUsersPerSec(userCount).during(duration))
                              .protocols(connectionFactory.buildAmqpProtocolBuilder(VHOST))
            ).maxDuration(duration + PAUSE_SECONDS + 10L);

        } catch (Exception e) {
            log.error("Unexpected error during load test", e);
        }
    }
}


