package loadtesting.scenarios;

public class SamuSmurScenario extends BaseScenario {

    protected ScenarioConfig getConfig() {
        return new ScenarioConfig(
                "15-smur",
                "15-smur: create case",
                "15-smur_v1.7",
                "fr.health.test.samu1-v3",
                "fr.health.test.samu2-v3",
                "rs-eda.json",
                "SAMU_SMUR_SCENARIO_USER_COUNT"
        );
    }
}