package loadtesting.simulations;

import loadtesting.amqp_scenario.BaseSimulation;
import loadtesting.amqp_scenario.ScenarioConfig;

public class SamuNexsisDirectSimulation extends BaseSimulation {

    protected ScenarioConfig getConfig() {
        return new ScenarioConfig(
                "15-18: direct transfer",
                "15-nexsis_v1.9",
                "fr.health.test.samuRC",
                "fr.fire.nexsis.sdisZ",
                "rc-eda.json",
                "SAMU_NEXSIS_DIRECT_SCENARIO_USER_COUNT"
        );
    }
}