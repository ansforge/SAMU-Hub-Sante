package loadtesting.simulations;

import io.gatling.javaapi.core.PopulationBuilder;
import io.gatling.javaapi.core.Simulation;
import loadtesting.AmqpConnectionFactory;
import loadtesting.scenarios.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static io.gatling.javaapi.core.CoreDsl.constantUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.global;
import static loadtesting.ConfigUtils.getEnvVar;
import static loadtesting.ConfigUtils.getNumericEnvVar;
import static loadtesting.Constants.DEFAULT_PAUSE_DURATION;

public class TmcSimulation extends Simulation {
    private final static int defaultDuration = 10;
    private final static int defaultUserCount = 2;

    private static final List<TmcScenario> SCENARIOS = List.of(
            new SamuGpsScenario(),
            new SamuSmurScenario(),
            new SamuSamuDirectScenario(),
            new SamuSamuConversionScenario(),
            new SamuNexsisDirectScenario(),
            new SamuNexsisConversionScenario(),
            new SamuNexsisRsScenario(),
            new SamuNexsisRcRiScenario()
    );

    private final Logger log = LoggerFactory.getLogger(getClass());

    {
        try {
            int duration = getNumericEnvVar("SCENARIO_DURATION", defaultDuration);
            int pause = getNumericEnvVar("SCENARIO_PAUSE_SECONDS", DEFAULT_PAUSE_DURATION);
            Set<String> selected = parseSelectedScenarios(getEnvVar("TMC_SCENARIOS"));

            List<String> availableKeys = SCENARIOS.stream().map(TmcScenario::key).toList();
            List<String> unknownKeys = selected.stream().filter(key -> !availableKeys.contains(key)).sorted().toList();
            if (!unknownKeys.isEmpty()) {
                throw new IllegalArgumentException(String.format(
                        "Unknown scenario(s) in TMC_SCENARIOS: %s, available: %s", unknownKeys, availableKeys));
            }

            AmqpConnectionFactory connectionFactory = new AmqpConnectionFactory();
            List<PopulationBuilder> populations = new ArrayList<>();

            for (TmcScenario scenario : SCENARIOS) {
                int userCount = getNumericEnvVar(scenario.userCountEnvVar(), defaultUserCount);
                if (!selected.isEmpty() && !selected.contains(scenario.key())) {
                    log.info("Scenario {} skipped: not in TMC_SCENARIOS", scenario.key());
                    continue;
                }
                if (userCount <= 0) {
                    log.info("Scenario {} skipped: {}={}", scenario.key(), scenario.userCountEnvVar(), userCount);
                    continue;
                }
                log.info("Scenario {} selected: {} users/s on vhost {} during {}s",
                        scenario.key(), userCount, scenario.vhost(), duration);
                populations.add(
                        scenario.build()
                                .injectOpen(constantUsersPerSec(userCount).during(duration))
                                .protocols(connectionFactory.buildAmqpProtocolBuilder(scenario.vhost()))
                );
            }

            if (populations.isEmpty()) {
                throw new IllegalStateException("No scenario selected (TMC_SCENARIOS / *_USER_COUNT)");
            }

            setUp(populations)
                    .maxDuration(duration * 2L + pause)
                    .assertions(global().failedRequests().count().is(0L));
        } catch (Exception e) {
            log.error("Unexpected error during load test", e);
            throw new IllegalStateException("Simulation setup failed", e);
        }
    }

    private static Set<String> parseSelectedScenarios(String value) {
        if (value == null) {
            return Set.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(key -> !key.isEmpty())
                .collect(Collectors.toSet());
    }
}
