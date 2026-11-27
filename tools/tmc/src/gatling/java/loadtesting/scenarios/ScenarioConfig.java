package loadtesting.scenarios;

import loadtesting.clients.ClientPool;
import loadtesting.clients.ClientPropertiesRegistry;

import java.util.function.Function;

public record ScenarioConfig(
        String key,
        String title,
        String vhost,
        Function<ClientPropertiesRegistry, ClientPool> getSenders,
        Function<ClientPropertiesRegistry, ClientPool> getRecipients,
        String messageSampleName,
        String userCountEnvVarKey
) {}
