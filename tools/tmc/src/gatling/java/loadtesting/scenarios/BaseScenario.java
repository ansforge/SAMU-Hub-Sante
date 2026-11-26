package loadtesting.scenarios;

import io.gatling.javaapi.core.ScenarioBuilder;
import loadtesting.SimulationUtils;
import loadtesting.clients.ClientPairPicker;
import loadtesting.clients.ClientPropertiesRegistry;

import java.util.Iterator;
import java.util.Map;

import static io.gatling.javaapi.core.CoreDsl.scenario;
import static loadtesting.Constants.*;
import static org.galaxio.gatling.amqp.javaapi.AmqpDsl.amqp;

public abstract class BaseScenario implements TmcScenario {
    protected abstract ScenarioConfig getConfig();

    @Override
    public String key() {
        return getConfig().key();
    }

    @Override
    public String vhost() {
        return getConfig().vhost();
    }

    @Override
    public String userCountEnvVar() {
        return getConfig().userCountEnvVarKey();
    }

    @Override
    public ClientPairPicker clientPairPicker(ClientPropertiesRegistry registry) {
        ScenarioConfig config = getConfig();
        return new ClientPairPicker("scenario " + config.key(),
                config.getSenders().apply(registry), config.getRecipients().apply(registry));
    }

    @Override
    public ScenarioBuilder build(ClientPropertiesRegistry registry) throws Exception {
        ScenarioConfig config = getConfig();

        String fileContent = SimulationUtils.loadSampleFile(config.messageSampleName());

        Iterator<Map<String, Object>> messageFeeder =
                SimulationUtils.generateMessageFeeder(fileContent, clientPairPicker(registry)::pick);

        return scenario(config.title())
                .feed(
                        messageFeeder
                )
                .exec(
                        amqp(config.title())
                                .publish()
                                .topicExchange(HUBSANTE_EXCHANGE, SENDER_ID_ROUTING_KEY)
                                .textMessage(String.format("#{%s}", GATLING_EL_MESSAGE_KEY))
                                .contentType(JSON_CONTENT_TYPE)
                );
    }
}
