package loadtesting.simulations;

import io.gatling.javaapi.core.PopulationBuilder;
import io.gatling.javaapi.core.Simulation;
import loadtesting.AmqpConnectionFactory;
import loadtesting.SimulationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;


public class CompleteProductionSimulation extends Simulation {
    private final static int defaultDuration = 600; // 10 minutes

    private final Logger log = LoggerFactory.getLogger(getClass());

    {
        try {
            AmqpConnectionFactory connectionFactory = new AmqpConnectionFactory();
            int duration = SimulationUtils.getNumericEnvVar("SCENARIO_DURATION", defaultDuration);

            ArrayList<PopulationBuilder> scenarios = new ArrayList<>();
            scenarios.add(SamuGpsSimulation.setupScenarioPopulation(duration, connectionFactory));
            scenarios.addAll(SamuNexsisSimulation.setupScenarioPopulation(duration, connectionFactory));

            setUp(scenarios).maxDuration(duration * 2L);
        } catch (Exception e) {
            log.error("Unexpected error during load test", e);
        }
    }
}