package loadtesting.clients;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientPropertiesRegistryTest {

    @TempDir
    Path tempDir;

    @Test
    void loadsClientIdsAndPerimeters() throws Exception {
        ClientPropertiesRegistry registry = ClientPropertiesRegistry.load(resource("clients/clients.yaml"));

        assertThat(registry.all()).containsExactly(
                new ClientProperties("fr.health.test.samu1-v3", List.of(
                        new PerimeterDefinition("15-15", List.of("2.1")),
                        new PerimeterDefinition("15-gps", List.of("1.3")))),
                new ClientProperties("fr.fire.nexsis.sdisZ", List.of(
                        new PerimeterDefinition("15-nexsis", List.of("active")))));
        assertThat(registry.get("fr.fire.nexsis.sdisZ").perimeters())
                .containsExactly(new PerimeterDefinition("15-nexsis", List.of("active")));
    }

    @Test
    void unknownClientIsNull() throws Exception {
        ClientPropertiesRegistry registry = ClientPropertiesRegistry.load(resource("clients/clients.yaml"));

        assertThat(registry.get("fr.health.unknown")).isNull();
    }

    @Test
    void missingFileFails() {
        Path missing = tempDir.resolve("absent.yaml");

        assertThatThrownBy(() -> ClientPropertiesRegistry.load(missing))
                .isInstanceOf(ClientConfigurationException.class)
                .hasMessageContaining(missing.toString());
    }

    @Test
    void invalidYamlFails() throws IOException {
        Path file = write("clients: [\n  - client_id: a\n");

        assertThatThrownBy(() -> ClientPropertiesRegistry.load(file))
                .isInstanceOf(ClientConfigurationException.class)
                .hasMessageContaining(file.toString());
    }

    @Test
    void emptyFileFails() throws IOException {
        Path file = write("");

        assertThatThrownBy(() -> ClientPropertiesRegistry.load(file))
                .isInstanceOf(ClientConfigurationException.class)
                .hasMessageContaining(file.toString());
    }

    @Test
    void missingClientsBlockFails() throws IOException {
        Path file = write("other: value\n");

        assertThatThrownBy(() -> ClientPropertiesRegistry.load(file))
                .isInstanceOf(ClientConfigurationException.class)
                .hasMessageContaining("Missing 'clients' block")
                .hasMessageContaining(file.toString());
    }

    @Test
    void nullClientsBlockFails() throws IOException {
        Path file = write("clients:\n");

        assertThatThrownBy(() -> ClientPropertiesRegistry.load(file))
                .isInstanceOf(ClientConfigurationException.class)
                .hasMessageContaining("Missing 'clients' block")
                .hasMessageContaining(file.toString());
    }

    @Test
    void clientsBlockThatIsNotAListFails() throws IOException {
        Path file = write("clients: fr.health.test.samu1-v3\n");

        assertThatThrownBy(() -> ClientPropertiesRegistry.load(file))
                .isInstanceOf(ClientConfigurationException.class)
                .hasMessageContaining(file.toString());
    }

    @Test
    void blankClientIdFails() throws IOException {
        Path file = write("""
                clients:
                  - client_id: '  '
                    perimeters:
                      - name: 15-15
                        versions: ["2.1"]
                """);

        assertThatThrownBy(() -> ClientPropertiesRegistry.load(file))
                .isInstanceOf(ClientConfigurationException.class)
                .hasMessageContaining("ClientId is missing in configuration");
    }

    @Test
    void invalidClientsAreAllReportedPerClient() throws IOException {
        Path file = write("""
                clients:
                  - client_name: no-id
                    perimeters:
                      - name: 15-15
                        versions: ["2.1"]
                  - client_id: fr.health.test.no-perimeter
                  - client_id: fr.health.test.bad-perimeters
                    perimeters:
                      - versions: ["2.1"]
                      - name: 15-gps
                """);

        assertThatThrownBy(() -> ClientPropertiesRegistry.load(file))
                .isInstanceOf(ClientConfigurationException.class)
                .hasMessageContaining("Invalid clients configuration in " + file)
                .hasMessageContaining("Client: null\n  - ClientId is missing in configuration")
                .hasMessageContaining("Client: fr.health.test.no-perimeter\n  - At least one perimeter must be configured")
                .hasMessageContaining("Client: fr.health.test.bad-perimeters\n"
                        + "  - Perimeter name must not be blank\n"
                        + "  - Perimeter versions must not be empty");
    }

    @Test
    void emptyClientOrPerimeterEntriesFail() throws IOException {
        Path file = write("""
                clients:
                  -
                  - client_id: fr.health.test.samu1-v3
                    perimeters:
                      -
                """);

        assertThatThrownBy(() -> ClientPropertiesRegistry.load(file))
                .isInstanceOf(ClientConfigurationException.class)
                .hasMessageContaining("Invalid clients configuration in " + file)
                .hasMessageContaining("Client: null\n  - Client entry is empty")
                .hasMessageContaining("Client: fr.health.test.samu1-v3\n  - Perimeter entry is empty");
    }

    @Test
    void duplicateClientIdFails() throws IOException {
        Path file = write("""
                clients:
                  - client_id: fr.health.test.samu1-v3
                    perimeters:
                      - name: 15-15
                        versions: ["2.1"]
                  - client_id: fr.health.test.samu1-v3
                    perimeters:
                      - name: 15-gps
                        versions: ["1.3"]
                """);

        assertThatThrownBy(() -> ClientPropertiesRegistry.load(file))
                .isInstanceOf(ClientConfigurationException.class)
                .hasMessageContaining("Client: fr.health.test.samu1-v3\n  - ClientId is duplicated in configuration");
    }

    @Test
    void configurationPathDefaultsToLocalFile() {
        assertThat(ClientPropertiesRegistry.configurationPath(null)).isEqualTo(Path.of("config/clients.yaml"));
        assertThat(ClientPropertiesRegistry.configurationPath(" ")).isEqualTo(Path.of("config/clients.yaml"));
    }

    @Test
    void configurationPathUsesProvidedValue() {
        assertThat(ClientPropertiesRegistry.configurationPath("/tmp/clients.yaml"))
                .isEqualTo(Path.of("/tmp/clients.yaml"));
    }

    private Path resource(String name) throws URISyntaxException {
        return Path.of(getClass().getClassLoader().getResource(name).toURI());
    }

    private Path write(String content) throws IOException {
        return Files.writeString(tempDir.resolve("clients.yaml"), content);
    }
}
