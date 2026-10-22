package loadtesting.scenarios;

public record ScenarioConfig(
        String key,
        String title,
        String vhost,
        String senderId,
        String recipientId,
        String messageSampleName,
        String userCountEnvVarKey
) {}
