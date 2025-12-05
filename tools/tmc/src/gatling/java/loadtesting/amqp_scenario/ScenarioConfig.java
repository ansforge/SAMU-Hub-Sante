package loadtesting.amqp_scenario;

public record ScenarioConfig(
        String title,
        String vhost,
        String senderId,
        String recipientId,
        String messageSampleName,
        String userCountEnvVarKey
) {}