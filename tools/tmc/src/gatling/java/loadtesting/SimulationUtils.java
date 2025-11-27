package loadtesting;

import io.github.cdimascio.dotenv.Dotenv;
import org.galaxio.gatling.amqp.javaapi.request.PublishDslBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static loadtesting.Constants.JSON_CONTENT_TYPE;
import static org.galaxio.gatling.amqp.javaapi.AmqpDsl.amqp;

public final class SimulationUtils {
    private static final Dotenv dotenv = Dotenv.load();
    private static final Logger log = LoggerFactory.getLogger(SimulationUtils.class);

    private static final String EXCHANGE_NAME_ENV_VAR = "EXCHANGE_NAME";
    private static final String EXCHANGE_NAME = dotenv.get(EXCHANGE_NAME_ENV_VAR);

    public static int getNumericEnvVar(String key, int defaultValue) {
        String envValue = dotenv.get(key);
        if (envValue != null) {
            try {
                return Integer.parseInt(envValue);
            } catch (NumberFormatException e) {
                String warningMessage = String.format("Failed to parse value provided for system variable %s", key);
                log.warn(warningMessage, e);
            }
        }
        return defaultValue;
    }

    public static String loadSampleMessage(String fileName) throws Exception {
        InputStream fileStream = SimulationUtils.class.getClassLoader().getResourceAsStream("messages/" + fileName);
        if (fileStream == null) throw new IOException("Resource not found:" + fileName);
        return new String(fileStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    public static PublishDslBuilder sendAmqpMessage(String clientId, String messageString) {
        return amqp("publish message as client " + clientId + " to exchange " + EXCHANGE_NAME)
                .publish()
                .topicExchange(EXCHANGE_NAME, clientId)
                .textMessage(messageString)
                .contentType(JSON_CONTENT_TYPE);
    }
}