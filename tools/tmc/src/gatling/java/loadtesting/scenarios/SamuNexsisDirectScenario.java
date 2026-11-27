package loadtesting.scenarios;

import loadtesting.clients.ClientPool;
import loadtesting.clients.ClientPropertiesRegistry;

import static loadtesting.Constants.NEXSIS_CLIENT_ID;

public class SamuNexsisDirectScenario extends BaseScenario {

    protected ScenarioConfig getConfig() {
        return new ScenarioConfig(
                "15-18-direct",
                "15-18: direct transfer",
                "15-nexsis_v1.9",
                ClientPropertiesRegistry::getDirectCisuClients,
                registry -> ClientPool.of(NEXSIS_CLIENT_ID),
                "rc-eda.json",
                "SAMU_NEXSIS_DIRECT_SCENARIO_USER_COUNT"
        );
    }
}