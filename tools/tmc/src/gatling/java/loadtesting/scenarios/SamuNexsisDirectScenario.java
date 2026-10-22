package loadtesting.scenarios;

public class SamuNexsisDirectScenario extends BaseScenario {

    protected ScenarioConfig getConfig() {
        return new ScenarioConfig(
                "15-18-direct",
                "15-18: direct transfer",
                "15-nexsis_v1.9",
                "fr.health.test.samu-direct-cisu-v3",
                "fr.fire.nexsis.sdisZ",
                "rc-eda.json",
                "SAMU_NEXSIS_DIRECT_SCENARIO_USER_COUNT"
        );
    }
}