package loadtesting.scenarios;

import io.gatling.javaapi.core.ScenarioBuilder;
import loadtesting.SimulationUtils;

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
    public ScenarioBuilder build() throws Exception {
        ScenarioConfig config = getConfig();

        String fileContent = SimulationUtils.loadSampleFile(config.messageSampleName());

        Iterator<Map<String, Object>> messageFeeder =
                SimulationUtils.generateMessageFeeder(fileContent, config.senderId(), config.recipientId());

        return scenario(config.title())
                .feed(
                        messageFeeder
                )
                .exec(
                        amqp(config.title())
                                .publish()
                                .topicExchange(HUBSANTE_EXCHANGE, config.senderId())
                                .textMessage(String.format("#{%s}", GATLING_EL_MESSAGE_KEY))
                                .contentType(JSON_CONTENT_TYPE)
                );
    }
}
