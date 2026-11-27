package loadtesting.clients;

import java.util.List;

public record ClientPool(String description, List<String> clientIds) {
    public ClientPool {
        clientIds = List.copyOf(clientIds);
    }

    public static ClientPool of(String clientId) {
        return new ClientPool(clientId, List.of(clientId));
    }
}
