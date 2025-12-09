package loadtesting.simulations;

import loadtesting.amqp_scenario.BaseSimulation;
import loadtesting.amqp_scenario.ScenarioConfig;

public class SamuSmurSimulation extends BaseSimulation {

    protected ScenarioConfig getConfig() {
        return new ScenarioConfig(
                "15-smur: create case",
                "15-smur_v1.7",
                "fr.health.test.samuv3",
                "fr.health.test.samuA",
                "rs-eda.json",
                "SAMU_SMUR_SCENARIO_USER_COUNT"
        );
    }
}