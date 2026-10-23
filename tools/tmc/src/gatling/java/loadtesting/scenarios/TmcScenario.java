package loadtesting.scenarios;

import io.gatling.javaapi.core.ScenarioBuilder;

public interface TmcScenario {
    String key();

    String vhost();

    String userCountEnvVar();

    ScenarioBuilder build() throws Exception;
}
