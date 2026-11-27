package loadtesting.scenarios;

import io.gatling.javaapi.core.ScenarioBuilder;
import loadtesting.SimulationUtils;
import loadtesting.clients.ClientPairPicker;
import loadtesting.clients.ClientPool;
import loadtesting.clients.ClientPropertiesRegistry;

import static io.gatling.javaapi.core.CoreDsl.*;
import static loadtesting.ConfigUtils.getNumericEnvVar;
import static loadtesting.Constants.*;
import static org.galaxio.gatling.amqp.javaapi.AmqpDsl.amqp;

/** Load test for the RS persistence flow (15-18 direction, SAMU → NexSIS).
 * Each virtual user sends RS-RI, waits SCENARIO_PAUSE_SECONDS, then sends RS-SR with the same caseId. */
public class SamuNexsisRsScenario implements TmcScenario {

    private static final String KEY = "15-18-persistance";
    private static final String TITLE = "RS flux complet — SAMU→NexSIS";
    private static final String VHOST = "15-15_v2.1";
    private static final String USER_COUNT_ENV_VAR = "SAMU_NEXSIS_RS_SCENARIO_USER_COUNT";

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
    public ClientPairPicker clientPairPicker(ClientPropertiesRegistry registry) {
        return new ClientPairPicker("scenario " + KEY, registry.getTranscodingCisuClients(), ClientPool.of(NEXSIS_CLIENT_ID));
    }

    @Override
    public ScenarioBuilder build(ClientPropertiesRegistry registry) throws Exception {
        String rsRiContent = SimulationUtils.loadSampleFile("rs-ri.json");
        String rsSrContent = SimulationUtils.loadSampleFile("rs-sr.json");

        int pause = getNumericEnvVar("SCENARIO_PAUSE_SECONDS", DEFAULT_PAUSE_DURATION);

        return scenario(TITLE)
                .feed(SimulationUtils.generateRsFlowFeeder(rsRiContent, rsSrContent, clientPairPicker(registry)::pick))
                .exec(
                        amqp(String.format("%s (RS-RI)", TITLE))
                                .publish()
                                .topicExchange(HUBSANTE_EXCHANGE, SENDER_ID_ROUTING_KEY)
                                .textMessage("#{rsRiMessage}")
                                .contentType(JSON_CONTENT_TYPE)
                )
                .pause(pause)
                .exec(
                        amqp(String.format("%s (RS-SR)", TITLE))
                                .publish()
                                .topicExchange(HUBSANTE_EXCHANGE, SENDER_ID_ROUTING_KEY)
                                .textMessage("#{rsSrMessage}")
                                .contentType(JSON_CONTENT_TYPE)
                );
    }
}
