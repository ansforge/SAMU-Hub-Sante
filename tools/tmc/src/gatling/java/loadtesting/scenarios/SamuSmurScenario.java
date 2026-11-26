package loadtesting.scenarios;

import loadtesting.clients.ClientPropertiesRegistry;

public class SamuSmurScenario extends BaseScenario {

    protected ScenarioConfig getConfig() {
        return new ScenarioConfig(
                "15-smur",
                "15-smur: create case",
                "15-smur_v1.7",
                ClientPropertiesRegistry::getSmurClients,
                ClientPropertiesRegistry::getSmurClients,
                "rs-eda.json",
                "SAMU_SMUR_SCENARIO_USER_COUNT"
        );
    }
}