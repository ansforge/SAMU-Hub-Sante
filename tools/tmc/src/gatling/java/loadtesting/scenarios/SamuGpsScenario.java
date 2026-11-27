package loadtesting.scenarios;

import loadtesting.clients.ClientPropertiesRegistry;

public class SamuGpsScenario extends BaseScenario {
    protected ScenarioConfig getConfig() {
        return new ScenarioConfig(
                "15-gps",
                "15-GPS: geo position update",
                "15-gps_v1.3",
                ClientPropertiesRegistry::getGpsClients,
                ClientPropertiesRegistry::getGpsClients,
                "geo-position.json",
                "SAMU_GPS_SCENARIO_USER_COUNT"
        );
    }
}