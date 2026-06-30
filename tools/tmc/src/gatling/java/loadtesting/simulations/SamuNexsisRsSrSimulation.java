package loadtesting.simulations;

import loadtesting.amqp_scenario.BaseSimulation;
import loadtesting.amqp_scenario.ScenarioConfig;

/**
 * Load test for the RS-SR persistence path on the 15-18 perimeter.
 */
public class SamuNexsisRsSrSimulation extends BaseSimulation {

    @Override
    protected ScenarioConfig getConfig() {
        return new ScenarioConfig(
                "15-18: RS-SR persistence (SAMU→NexSIS)",
                "15-15_v2.1",
                "fr.health.test.samu1-v3",
                "fr.fire.nexsis.sdisZ",
                "rs-sr.json",
                "SAMU_NEXSIS_RS_SR_SCENARIO_USER_COUNT"
        );
    }
}
