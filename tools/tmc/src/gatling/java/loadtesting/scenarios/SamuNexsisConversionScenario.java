package loadtesting.scenarios;

import loadtesting.clients.ClientPool;
import loadtesting.clients.ClientPropertiesRegistry;

import static loadtesting.Constants.NEXSIS_CLIENT_ID;

public class SamuNexsisConversionScenario extends BaseScenario {

    protected ScenarioConfig getConfig() {
        return new ScenarioConfig(
                "15-18-conversion",
                "15-18: conversion transfer",
                "15-15_v2.1",
                ClientPropertiesRegistry::getTranscodingCisuClients,
                registry -> ClientPool.of(NEXSIS_CLIENT_ID),
                "rs-eda.json",
                "SAMU_NEXSIS_CONVERSION_SCENARIO_USER_COUNT"
        );
    }
}