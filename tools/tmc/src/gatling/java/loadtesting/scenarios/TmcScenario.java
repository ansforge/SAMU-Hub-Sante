package loadtesting.scenarios;

import io.gatling.javaapi.core.ScenarioBuilder;
import loadtesting.clients.ClientPairPicker;
import loadtesting.clients.ClientPropertiesRegistry;

public interface TmcScenario {
    String key();

    String vhost();

    String userCountEnvVar();

    ClientPairPicker clientPairPicker(ClientPropertiesRegistry registry);

    ScenarioBuilder build(ClientPropertiesRegistry registry) throws Exception;
}
