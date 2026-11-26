package loadtesting.scenarios;

import loadtesting.clients.ClientPropertiesRegistry;

public class SamuSamuConversionScenario extends BaseScenario {

    protected ScenarioConfig getConfig() {
        return new ScenarioConfig(
                "15-15-conversion",
                "15-15: conversion transfer",
                "15-15_v2.1",
                ClientPropertiesRegistry::getSamuV3Clients,
                ClientPropertiesRegistry::getSamuV1Clients,
                "rs-eda.json",
                "SAMU_SAMU_CONVERSION_SCENARIO_USER_COUNT"
        );
    }
}