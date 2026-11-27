package loadtesting.clients;

public class ClientConfigurationException extends RuntimeException {
    public ClientConfigurationException(String message) {
        super(message);
    }

    public ClientConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
