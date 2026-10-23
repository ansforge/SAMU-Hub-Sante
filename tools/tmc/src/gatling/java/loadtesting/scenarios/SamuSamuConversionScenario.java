package loadtesting.scenarios;

public class SamuSamuConversionScenario extends BaseScenario {

    protected ScenarioConfig getConfig() {
        return new ScenarioConfig(
                "15-15-conversion",
                "15-15: conversion transfer",
                "15-15_v2.1",
                "fr.health.test.samu1-v3",
                "fr.health.test.samu1-v1",
                "rs-eda.json",
                "SAMU_SAMU_CONVERSION_SCENARIO_USER_COUNT"
        );
    }
}