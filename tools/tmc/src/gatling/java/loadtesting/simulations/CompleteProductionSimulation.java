package loadtesting.simulations;

import loadtesting.AmqpSimulation;
import org.galaxio.gatling.amqp.javaapi.protocol.AmqpProtocolBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;

public class CompleteProductionSimulation extends AmqpSimulation {
    @Override
    protected void setupScenarios() throws Exception {
        String messageString = loadSampleMessage("rs-eda.json");
        String invalidMessageString = loadSampleMessage("invalid.json");
        String conversionMessageString = loadSampleMessage("conversion.json");
        String traductionMessageString = loadSampleMessage("traduction.json");

        AmqpProtocolBuilder samuAConnection = amqpConfFactory("15-15_v1.5");
        AmqpProtocolBuilder samuv1Connection = amqpConfFactory("15-15_v1.5");
        AmqpProtocolBuilder samuv3Connection = amqpConfFactory("15-15_v2.1");
        AmqpProtocolBuilder samuBConnection = amqpConfFactory("15-nexsis_v1.9");

        ScenarioBuilder standardScenario = buildAMPQScenario("RS-EDA", "fr.health.test.samuA", messageString);
        ScenarioBuilder conversionScenario = buildAMPQScenario("Convert RS-EDA v1 to v3", "fr.health.test.samuv1", conversionMessageString);
        ScenarioBuilder translationScenario = buildAMPQScenario("Translate RS-EDA to RC-EDA", "fr.health.test.samuv3", traductionMessageString);
        ScenarioBuilder invalidMessageScenario = buildAMPQScenario("Invalid message", "fr.health.test.samuB", invalidMessageString);

        setUp(
                setupScenario(conversionScenario, samuv1Connection, 5),
                setupScenario(standardScenario, samuAConnection, 10),
                setupScenario(translationScenario, samuv3Connection, 5),
                setupScenario(invalidMessageScenario, samuBConnection, 5)
        )
                .maxDuration(600);
    }
}