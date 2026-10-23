package loadtesting.scenarios;

import io.gatling.javaapi.core.ScenarioBuilder;
import loadtesting.SimulationUtils;

import static io.gatling.javaapi.core.CoreDsl.*;
import static loadtesting.ConfigUtils.getNumericEnvVar;
import static loadtesting.Constants.*;
import static org.galaxio.gatling.amqp.javaapi.AmqpDsl.amqp;

/** Load test for the RC-RI persistence flow (18-15 direction, NexSIS → SAMU).
 * Each virtual user sends a RC-RI with a fresh caseId (new case path), waits SCENARIO_PAUSE_SECONDS,
 * then sends a second RC-RI with the same caseId (known case path, triggers diff). */
public class SamuNexsisRcRiScenario implements TmcScenario {

    private static final String KEY = "18-15-persistance";
    private static final String TITLE = "RC-RI flux complet — NexSIS→SAMU";
    private static final String VHOST = "15-nexsis_vactive";
    private static final String SENDER_ID = "fr.fire.nexsis.sdisZ";
    private static final String RECIPIENT_ID = "fr.health.test.samu1-v3";
    private static final String USER_COUNT_ENV_VAR = "SAMU_NEXSIS_RC_RI_SCENARIO_USER_COUNT";

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String vhost() {
        return VHOST;
    }

    @Override
    public String userCountEnvVar() {
        return USER_COUNT_ENV_VAR;
    }

    @Override
    public ScenarioBuilder build() throws Exception {
        String rcRiContent = SimulationUtils.loadSampleFile("rc-ri.json");

        int pause = getNumericEnvVar("SCENARIO_PAUSE_SECONDS", DEFAULT_PAUSE_DURATION);

        return scenario(TITLE)
                .feed(SimulationUtils.generateRcRiFlowFeeder(rcRiContent, SENDER_ID, RECIPIENT_ID))
                .exec(
                        amqp(String.format("%s (RC-RI nouveau caseId)", TITLE))
                                .publish()
                                .topicExchange(HUBSANTE_EXCHANGE, SENDER_ID)
                                .textMessage("#{rcRiNewMessage}")
                                .contentType(JSON_CONTENT_TYPE)
                )
                .pause(pause)
                .exec(
                        amqp(String.format("%s (RC-RI mise à jour)", TITLE))
                                .publish()
                                .topicExchange(HUBSANTE_EXCHANGE, SENDER_ID)
                                .textMessage("#{rcRiUpdateMessage}")
                                .contentType(JSON_CONTENT_TYPE)
                );
    }
}
