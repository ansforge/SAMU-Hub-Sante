package loadtesting.scenarios;

public class SamuSamuDirectScenario extends BaseScenario {

    protected ScenarioConfig getConfig() {
        return new ScenarioConfig(
                "15-15-direct",
                "15-15: direct transfer",
                "15-15_v2.1",
                "fr.health.test.samu1-v3",
                "fr.health.test.samu2-v3",
                "rs-eda.json",
                "SAMU_SAMU_DIRECT_SCENARIO_USER_COUNT"
        );
    }
}