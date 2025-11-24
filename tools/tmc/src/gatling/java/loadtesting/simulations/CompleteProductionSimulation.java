package loadtesting.simulations;

import loadtesting.AmqpSimulation;
import org.galaxio.gatling.amqp.javaapi.protocol.AmqpProtocolBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;

import static io.gatling.javaapi.core.CoreDsl.constantUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.scenario;

public class CompleteProductionSimulation extends AmqpSimulation {
    @Override
    protected void setupScenarios() throws Exception {
        String messageString = loadSampleMessage("rs-eda.json");
        String invalidMessageString = loadSampleMessage("invalid.json");
        String conversionMessageString = loadSampleMessage("conversion.json");
        String traductionMessageString = loadSampleMessage("traduction.json");

        AmqpProtocolBuilder samuAConnection = amqpConnectionWrapper("15-15_v1.5");
        AmqpProtocolBuilder samuv1Connection = amqpConnectionWrapper("15-15_v1.5");
        AmqpProtocolBuilder samuv3Connection = amqpConnectionWrapper("15-15_v2.1");
        AmqpProtocolBuilder samuBConnection = amqpConnectionWrapper("15-nexsis_v1.9");

        ScenarioBuilder standardScenario = scenario("RS-EDA").exec(sendAmqpMessage( "fr.health.test.samuA", messageString));
        ScenarioBuilder conversionScenario = scenario("Convert RS-EDA v1 to v3").exec(sendAmqpMessage("fr.health.test.samuv1", conversionMessageString));
        ScenarioBuilder translationScenario = scenario("Translate RS-EDA to RC-EDA").exec(sendAmqpMessage("fr.health.test.samuv3", traductionMessageString));
        ScenarioBuilder invalidMessageScenario = scenario("Invalid message").exec(sendAmqpMessage("fr.health.test.samuB", invalidMessageString));

        setUp(
                conversionScenario.injectOpen(
                        constantUsersPerSec(5).during(180)
                ).protocols(samuv1Connection),
                standardScenario.injectOpen(
                        constantUsersPerSec(10).during(180)
                ).protocols(samuAConnection),
                translationScenario.injectOpen(
                        constantUsersPerSec(5).during(180)
                ).protocols(samuv3Connection),
                invalidMessageScenario.injectOpen(
                        constantUsersPerSec(5).during(180)
                ).protocols(samuBConnection)
        )
                .maxDuration(600);
    }
}