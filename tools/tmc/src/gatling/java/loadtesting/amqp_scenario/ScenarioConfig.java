package loadtesting.amqp_scenario;

public record ScenarioConfig(
        String title,
        String vhost,
        String senderId,
        String recipientId,
        String messageSampleName,
        String userCountEnvVarKey,
        CaseIdStrategy caseIdStrategy
) {
    public enum CaseIdStrategy {
        // A fresh UUID-based caseId is generated for every single message.
        UNIQUE,
        // The caseId embedded in the sample file is reused as-is for every message.
        FIXED
    }

    // Backward-compatible constructor: defaults to CaseIdStrategy.FIXED
    public ScenarioConfig(
            String title,
            String vhost,
            String senderId,
            String recipientId,
            String messageSampleName,
            String userCountEnvVarKey) {
        this(title, vhost, senderId, recipientId, messageSampleName,
                userCountEnvVarKey, CaseIdStrategy.FIXED);
    }
}