package loadtesting.simulations;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import loadtesting.AmqpConnectionFactory;
import loadtesting.SimulationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static io.gatling.javaapi.core.CoreDsl.*;
import static loadtesting.ConfigUtils.getNumericEnvVar;
import static loadtesting.Constants.*;
import static org.galaxio.gatling.amqp.javaapi.AmqpDsl.amqp;

/**
 * Load test for the RC-RI "known case" path on the 15-18 perimeter (NexSIS → SAMU direction).
 */
public class SamuNexsisRcRiKnownCaseSimulation extends Simulation {

    private static final Logger log = LoggerFactory.getLogger(SamuNexsisRcRiKnownCaseSimulation.class);

    private static final String VHOST = "15-nexsis_v1.9";
    private static final String SENDER_ID = "fr.fire.nexsis.sdisZ";
    private static final String RECIPIENT_ID = "fr.health.test.samu1-v3";
    private static final String USER_COUNT_ENV_VAR = "SAMU_NEXSIS_RC_RI_KNOWN_CASE_SCENARIO_USER_COUNT";

    /** Pause (seconds) between warmup and load phase to let the Dispatcher persist all messages. */
    private static final int WARMUP_PAUSE_SECONDS = 10;

    /**
     * Fixed pool of caseIds shared between the warmup and the load phase.
     * Size should be >= the number of concurrent Gatling workers to spread DB reads.
     */
    private static final List<String> CASE_ID_POOL = List.of(
            "fr.fire.nexsis.sdisZ.TMC_KNOWN_001",
            "fr.fire.nexsis.sdisZ.TMC_KNOWN_002",
            "fr.fire.nexsis.sdisZ.TMC_KNOWN_003",
            "fr.fire.nexsis.sdisZ.TMC_KNOWN_004",
            "fr.fire.nexsis.sdisZ.TMC_KNOWN_005",
            "fr.fire.nexsis.sdisZ.TMC_KNOWN_006",
            "fr.fire.nexsis.sdisZ.TMC_KNOWN_007",
            "fr.fire.nexsis.sdisZ.TMC_KNOWN_008",
            "fr.fire.nexsis.sdisZ.TMC_KNOWN_009",
            "fr.fire.nexsis.sdisZ.TMC_KNOWN_010"
    );

    {
        try {
            String fileContent = SimulationUtils.loadSampleFile("rc-ri.json");

            // Warmup feeder: one entry per caseId in the pool, consumed once
            Iterator<Map<String, Object>> warmupFeeder =
                    SimulationUtils.generateFixedPoolMessageFeeder(fileContent, SENDER_ID, RECIPIENT_ID, CASE_ID_POOL);

            // Load feeder: round-robin over the same pool indefinitely
            Iterator<Map<String, Object>> loadFeeder =
                    SimulationUtils.generateRoundRobinMessageFeeder(fileContent, SENDER_ID, RECIPIENT_ID, CASE_ID_POOL);

            ScenarioBuilder warmupScenario = scenario("RC-RI known case — warmup")
                    .feed(warmupFeeder)
                    .exec(
                            amqp(String.format("%s %s (warmup)", AMQP_REQUEST_NAME, SENDER_ID))
                                    .publish()
                                    .topicExchange(HUBSANTE_EXCHANGE, SENDER_ID)
                                    .textMessage(String.format("#{%s}", GATLING_EL_MESSAGE_KEY))
                                    .contentType(JSON_CONTENT_TYPE)
                    );

            ScenarioBuilder loadScenario = scenario("RC-RI known case — load phase")
                    .feed(loadFeeder)
                    .exec(
                            amqp(String.format("%s %s", AMQP_REQUEST_NAME, SENDER_ID))
                                    .publish()
                                    .topicExchange(HUBSANTE_EXCHANGE, SENDER_ID)
                                    .textMessage(String.format("#{%s}", GATLING_EL_MESSAGE_KEY))
                                    .contentType(JSON_CONTENT_TYPE)
                    );

            AmqpConnectionFactory connectionFactory = new AmqpConnectionFactory();

            int duration = getNumericEnvVar("SCENARIO_DURATION", 10);
            int userCount = getNumericEnvVar(USER_COUNT_ENV_VAR, 2);

            setUp(
                    warmupScenario.injectOpen(
                            atOnceUsers(CASE_ID_POOL.size())
                    ),
                    loadScenario.injectOpen(
                            nothingFor(WARMUP_PAUSE_SECONDS),
                            constantUsersPerSec(userCount).during(duration)
                    )
            ).protocols(connectionFactory.buildAmqpProtocolBuilder(VHOST))
             .maxDuration(duration * 2L + WARMUP_PAUSE_SECONDS);

        } catch (Exception e) {
            log.error("Unexpected error during load test", e);
        }
    }
}
