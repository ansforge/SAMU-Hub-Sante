package loadtesting.simulations;

import loadtesting.amqp_scenario.BaseSimulation;
import loadtesting.amqp_scenario.ScenarioConfig;

/**
 * Load test for the RS-RI persistence path on the 15-18 perimeter.
 */
public class SamuNexsisRsRiSimulation extends BaseSimulation {

    @Override
    protected ScenarioConfig getConfig() {
        return new ScenarioConfig(
                "15-18: RS-RI persistence (SAMU→NexSIS)",
                "15-15_v2.1",
                "fr.health.test.samu1-v3",
                "fr.fire.nexsis.sdisZ",
                "rs-ri.json",
                "SAMU_NEXSIS_RS_RI_SCENARIO_USER_COUNT"
        );
    }
}
