package loadtesting;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for configuration management.
 * Provides methods to read configuration from environment variables with fallback to .env file.
 */
public final class ConfigUtils {
    private static final Logger log = LoggerFactory.getLogger(ConfigUtils.class);
    private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

    private ConfigUtils() {
        // Utility class, prevent instantiation
    }

    /**
     * Get environment variable value.
     * First checks system environment variables, then falls back to .env file.
     *
     * @param key the environment variable key
     * @return the value, or null if not found
     */
    public static String getEnvVar(String key) {
        // First check system environment variables
        String value = System.getenv(key);
        if (value != null) {
            return value;
        }
        // Fall back to .env file
        return dotenv.get(key);
    }

    /**
     * Get numeric environment variable with default fallback.
     *
     * @param key the environment variable key
     * @param defaultValue the default value if not found or parsing fails
     * @return the parsed integer value or default
     */
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
