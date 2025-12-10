package loadtesting;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class ConfigUtils {
    private static final Logger log = LoggerFactory.getLogger(ConfigUtils.class);
    private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

    public static String getEnvVar(String key) {
        // First check system environment variables
        String value = System.getenv(key);
        if (value != null) {
            return value;
        }
        // Fall back to .env file
        return dotenv.get(key);
    }

    public static int getNumericEnvVar(String key, int defaultValue) {
        String envValue = getEnvVar(key);
        if (envValue != null) {
            try {
                return Integer.parseInt(envValue);
            } catch (NumberFormatException e) {
                String warningMessage = String.format("Failed to parse value provided for environment variable %s", key);
                log.warn(warningMessage, e);
            }
        }
        return defaultValue;
    }
}
