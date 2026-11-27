package loadtesting.scenarios;

import loadtesting.clients.ClientPropertiesRegistry;

public class SamuSamuDirectScenario extends BaseScenario {

    protected ScenarioConfig getConfig() {
        return new ScenarioConfig(
                "15-15-direct",
                "15-15: direct transfer",
                "15-15_v2.1",
                ClientPropertiesRegistry::getSamuV3Clients,
                ClientPropertiesRegistry::getSamuV3Clients,
                "rs-eda.json",
                "SAMU_SAMU_DIRECT_SCENARIO_USER_COUNT"
        );
    }
}