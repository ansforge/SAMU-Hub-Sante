package loadtesting.simulations;

import loadtesting.amqp_scenario.BaseSimulation;
import loadtesting.amqp_scenario.ScenarioConfig;

/**
 * Load test for the RC-RI "new case" path on the 15-18 perimeter (NexSIS → SAMU).
 */
public class SamuNexsisRcRiNewCaseSimulation extends BaseSimulation {

    @Override
    protected ScenarioConfig getConfig() {
        return new ScenarioConfig(
                "15-18: RC-RI new case (NexSIS→SAMU, DB empty)",
                "15-nexsis_v1.9",
                "fr.fire.nexsis.sdisZ",
                "fr.health.test.samu1-v3",
                "rc-ri.json",
                "SAMU_NEXSIS_RC_RI_NEW_CASE_SCENARIO_USER_COUNT",
                ScenarioConfig.CaseIdStrategy.UNIQUE
        );
    }
}
