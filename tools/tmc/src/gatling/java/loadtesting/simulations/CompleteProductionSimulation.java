package loadtesting.simulations;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import loadtesting.AmqpConnectionFactory;
import loadtesting.SimulationUtils;
import org.galaxio.gatling.amqp.javaapi.protocol.AmqpProtocolBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.gatling.javaapi.core.CoreDsl.constantUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.scenario;


public class CompleteProductionSimulation extends Simulation {
    private final static int defaultDuration = 600; // 10 minutes

    private final Logger log = LoggerFactory.getLogger(getClass());

    {
        try {
            String messageString = SimulationUtils.loadSampleMessage("rs-eda.json");
            String invalidMessageString = SimulationUtils.loadSampleMessage("invalid.json");
            String conversionMessageString = SimulationUtils.loadSampleMessage("conversion.json");
            String traductionMessageString = SimulationUtils.loadSampleMessage("traduction.json");

            AmqpConnectionFactory connectionFactory = new AmqpConnectionFactory();
            AmqpProtocolBuilder samuToSamuV1Connection = connectionFactory.buildAmqpProtocolBuilder("15-15_v1.5");
            AmqpProtocolBuilder samuToSamuV3Connection = connectionFactory.buildAmqpProtocolBuilder("15-15_v2.1");
            AmqpProtocolBuilder samuToNexsisV3Connection = connectionFactory.buildAmqpProtocolBuilder("15-nexsis_v1.9");
            AmqpProtocolBuilder samuGpsConnection = connectionFactory.buildAmqpProtocolBuilder("15-gps_v1.3");

            ScenarioBuilder standardScenario = scenario("RS-EDA").exec(SimulationUtils.sendAmqpMessage("fr.health.test.samuA", messageString));
            ScenarioBuilder conversionScenario = scenario("Convert RS-EDA v1 to v3").exec(SimulationUtils.sendAmqpMessage("fr.health.test.samuv1", conversionMessageString));
            ScenarioBuilder translationScenario = scenario("Translate RS-EDA to RC-EDA").exec(SimulationUtils.sendAmqpMessage("fr.health.test.samuv3", traductionMessageString));
            ScenarioBuilder invalidMessageScenario = scenario("Invalid message").exec(SimulationUtils.sendAmqpMessage("fr.health.test.samuB", invalidMessageString));

            int duration = SimulationUtils.getNumericEnvVar("SCENARIO_DURATION", defaultDuration);

            setUp(
                    conversionScenario.injectOpen(
                            constantUsersPerSec(5).during(duration)
                    ).protocols(samuToSamuV1Connection),
                    standardScenario.injectOpen(
                            constantUsersPerSec(10).during(duration)
                    ).protocols(samuToSamuV1Connection),
                    translationScenario.injectOpen(
                            constantUsersPerSec(5).during(duration)
                    ).protocols(samuToSamuV3Connection),
                    invalidMessageScenario.injectOpen(
                            constantUsersPerSec(5).during(duration)
                    ).protocols(samuToNexsisV3Connection),
            ).maxDuration(duration * 2L);
        } catch (Exception e) {
            log.error("Unexpected error during load test", e);
        }
    }
}