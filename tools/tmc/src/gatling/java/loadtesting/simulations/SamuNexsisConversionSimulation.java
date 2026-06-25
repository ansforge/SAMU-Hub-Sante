package loadtesting.simulations;

import loadtesting.amqp_scenario.BaseSimulation;
import loadtesting.amqp_scenario.ScenarioConfig;

public class SamuNexsisConversionSimulation extends BaseSimulation {

    protected ScenarioConfig getConfig() {
        return new ScenarioConfig(
                "15-18: conversion transfer",
                "15-15_v2.1",
                "fr.health.test.samu1-v3",
                "fr.fire.nexsis.sdisZ",
                "rs-eda.json",
                "SAMU_NEXSIS_CONVERSION_SCENARIO_USER_COUNT"
        );
    }
}