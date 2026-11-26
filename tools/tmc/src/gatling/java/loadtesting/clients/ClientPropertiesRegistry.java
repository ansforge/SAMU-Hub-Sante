package loadtesting.clients;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ClientPropertiesRegistry {
    public static final Path DEFAULT_CONFIGURATION_PATH = Path.of("config/clients.yaml");
    public static final String HEALTH_TEST_PREFIX = "fr.health.test.";

    private static final PerimeterVersion SAMU_V1 = new PerimeterVersion("15-15", "1.5");
    private static final PerimeterVersion SAMU_V3 = new PerimeterVersion("15-15", "2.1");
    private static final PerimeterVersion GPS_V3 = new PerimeterVersion("15-gps", "1.3");
    private static final PerimeterVersion SMUR_V3 = new PerimeterVersion("15-smur", "1.7");
    private static final PerimeterVersion NEXSIS_V3 = new PerimeterVersion("15-nexsis", "1.9");

    private static final ObjectMapper yamlMapper = JsonMapper.builder(new YAMLFactory())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private record ClientsConfiguration(List<ClientProperties> clients) {}

    private final List<ClientProperties> clients;
    private final Map<String, ClientProperties> clientsById;

    private ClientPropertiesRegistry(List<ClientProperties> clients) {
        this.clients = List.copyOf(clients);
        this.clientsById = clients.stream()
                .collect(Collectors.toUnmodifiableMap(ClientProperties::clientId, Function.identity()));
    }

    public static Path configurationPath(String configuredPath) {
        if (configuredPath == null || configuredPath.isBlank()) {
            return DEFAULT_CONFIGURATION_PATH;
        }
        return Path.of(configuredPath);
    }

    public static ClientPropertiesRegistry load(Path path) {
        ClientsConfiguration configuration;
        try {
            configuration = yamlMapper.readValue(path.toFile(), ClientsConfiguration.class);
        } catch (IOException e) {
            throw new ClientConfigurationException(
                    String.format("Unable to read clients configuration file %s: %s", path, e.getMessage()), e);
        }

        if (configuration == null || configuration.clients() == null) {
            throw new ClientConfigurationException(
                    String.format("Missing 'clients' block in clients configuration file %s", path));
        }

        validateClients(path, configuration.clients());
        return new ClientPropertiesRegistry(configuration.clients());
    }

    public ClientProperties get(String clientId) {
        return clientsById.get(clientId);
    }

    public List<ClientProperties> all() {
        return clients;
    }

    public ClientPool getClientsByPerimeterVersion(PerimeterVersion required, PerimeterVersion... excluded) {
        List<String> clientIds = clients.stream()
                .filter(client -> client.clientId().startsWith(HEALTH_TEST_PREFIX))
                .filter(client -> supports(client, required))
                .filter(client -> Arrays.stream(excluded).noneMatch(perimeterVersion -> supports(client, perimeterVersion)))
                .map(ClientProperties::clientId)
                .toList();

        StringBuilder description = new StringBuilder(HEALTH_TEST_PREFIX + "* with " + describe(required));
        Arrays.stream(excluded).forEach(perimeterVersion -> description.append(" without ").append(describe(perimeterVersion)));
        return new ClientPool(description.toString(), clientIds);
    }

    public ClientPool getGpsClients() {
        return getClientsByPerimeterVersion(GPS_V3);
    }

    public ClientPool getSmurClients() {
        return getClientsByPerimeterVersion(SMUR_V3);
    }

    public ClientPool getSamuV1Clients() {
        return getClientsByPerimeterVersion(SAMU_V1);
    }

    public ClientPool getSamuV3Clients() {
        return getClientsByPerimeterVersion(SAMU_V3);
    }

    public ClientPool getDirectCisuClients() {
        return getClientsByPerimeterVersion(NEXSIS_V3);
    }

    public ClientPool getTranscodingCisuClients() {
        return getClientsByPerimeterVersion(SAMU_V3, NEXSIS_V3);
    }

    private static String describe(PerimeterVersion perimeterVersion) {
        return perimeterVersion.perimeter() + "_v" + perimeterVersion.version();
    }

    private static boolean supports(ClientProperties client, PerimeterVersion perimeterVersion) {
        return client.perimeters().stream().anyMatch(perimeter ->
                perimeter.name().equals(perimeterVersion.perimeter())
                        && perimeter.versions().contains(perimeterVersion.version()));
    }

    private static void validateClients(Path path, List<ClientProperties> clients) {
        Map<String, List<String>> errorsByClient = new LinkedHashMap<>();
        Set<String> seenClientIds = new HashSet<>();

        for (ClientProperties client : clients) {
            if (client == null) {
                errorsByClient.computeIfAbsent(null, id -> new ArrayList<>()).add("Client entry is empty");
                continue;
            }
            List<String> errors = new ArrayList<>();

            if (client.clientId() == null || client.clientId().isBlank()) {
                errors.add("ClientId is missing in configuration");
            } else if (!seenClientIds.add(client.clientId())) {
                errors.add("ClientId is duplicated in configuration");
            }

            if (client.perimeters() == null || client.perimeters().isEmpty()) {
                errors.add("At least one perimeter must be configured");
            } else {
                for (PerimeterDefinition perimeter : client.perimeters()) {
                    if (perimeter == null) {
                        errors.add("Perimeter entry is empty");
                        continue;
                    }
                    if (perimeter.name() == null || perimeter.name().isBlank()) {
                        errors.add("Perimeter name must not be blank");
                    }
                    if (perimeter.versions() == null || perimeter.versions().isEmpty()) {
                        errors.add("Perimeter versions must not be empty");
                    }
                }
            }

            if (!errors.isEmpty()) {
                errorsByClient.computeIfAbsent(client.clientId(), id -> new ArrayList<>()).addAll(errors);
            }
        }

        if (!errorsByClient.isEmpty()) {
            StringBuilder sb = new StringBuilder(String.format("Invalid clients configuration in %s:\n", path));
            errorsByClient.forEach((clientId, errors) -> {
                sb.append("\nClient: ").append(clientId).append("\n");
                errors.forEach(error -> sb.append("  - ").append(error).append("\n"));
            });
            throw new ClientConfigurationException(sb.toString());
        }
    }
}
