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

/** Load test for the RC-RI persistence flow (18-15 direction, NexSIS → SAMU).
 * Each virtual user sends a RC-RI with a fresh caseId (new case path), waits SCENARIO_PAUSE_SECONDS,
 * then sends a second RC-RI with the same caseId (known case path, triggers diff). */
public class SamuNexsisRcRiSimulation extends Simulation {

    private static final Logger log = LoggerFactory.getLogger(SamuNexsisRcRiSimulation.class);

    private static final String VHOST = "15-nexsis_vactive";
    private static final String SENDER_ID = "fr.fire.nexsis.sdisZ";
    private static final String RECIPIENT_ID = "fr.health.test.samu1-v3";
    private static final String USER_COUNT_ENV_VAR = "SAMU_NEXSIS_RC_RI_SCENARIO_USER_COUNT";

    {
        try {
            String rcRiContent = SimulationUtils.loadSampleFile("rc-ri.json");

            int pause = getNumericEnvVar("SCENARIO_PAUSE_SECONDS", 5);

            ScenarioBuilder rcRiScenario = scenario("RC-RI flux complet — NexSIS→SAMU")
                    .feed(SimulationUtils.generateRcRiFlowFeeder(rcRiContent, SENDER_ID, RECIPIENT_ID))
                    .exec(
                            amqp(String.format("%s %s (RC-RI nouveau caseId)", AMQP_REQUEST_NAME, SENDER_ID))
                                    .publish()
                                    .topicExchange(HUBSANTE_EXCHANGE, SENDER_ID)
                                    .textMessage("#{rcRiNewMessage}")
                                    .contentType(JSON_CONTENT_TYPE)
                    )
                    .pause(pause)
                    .exec(
                            amqp(String.format("%s %s (RC-RI mise à jour)", AMQP_REQUEST_NAME, SENDER_ID))
                                    .publish()
                                    .topicExchange(HUBSANTE_EXCHANGE, SENDER_ID)
                                    .textMessage("#{rcRiUpdateMessage}")
                                    .contentType(JSON_CONTENT_TYPE)
                    );

            AmqpConnectionFactory connectionFactory = new AmqpConnectionFactory();

            int duration = getNumericEnvVar("SCENARIO_DURATION", 10);
            int userCount = getNumericEnvVar(USER_COUNT_ENV_VAR, 2);

            setUp(
                    rcRiScenario.injectOpen(constantUsersPerSec(userCount).during(duration))
                                .protocols(connectionFactory.buildAmqpProtocolBuilder(VHOST))
            ).maxDuration(duration * 2L);

        } catch (Exception e) {
            log.error("Unexpected error during load test", e);
        }
    }
}

