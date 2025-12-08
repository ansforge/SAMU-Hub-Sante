package loadtesting.simulations;

import loadtesting.amqp_scenario.BaseSimulation;
import loadtesting.amqp_scenario.ScenarioConfig;

public class SamuGpsSimulation extends BaseSimulation {
    protected ScenarioConfig getConfig() {
        return new ScenarioConfig(
                "15-GPS: geo position update",
                "15-gps_v1.3",
                "fr.health.test.samuA",
                "fr.health.test.samuC",
                "geo-position.json",
                "SAMU_GPS_SCENARIO_USER_COUNT"
        );
    }
}