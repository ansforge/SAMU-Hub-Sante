package loadtesting.scenarios;

import loadtesting.clients.ClientConfigurationException;
import loadtesting.clients.ClientPair;
import loadtesting.clients.ClientPropertiesRegistry;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TmcScenarioTest {
    private static final String SAMU1_V3 = "fr.health.test.samu1-v3";
    private static final String SAMU2_V3 = "fr.health.test.samu2-v3";
    private static final String SAMU1_V1 = "fr.health.test.samu1-v1";
    private static final String SAMU_DIRECT_CISU = "fr.health.test.samu-direct-cisu-v3";
    private static final String SAMU_HYBRID = "fr.health.test.samu-hybrid-v3";
    private static final String NEXSIS = "fr.fire.nexsis.sdisZ";

    @TempDir
    Path tempDir;

    static Stream<Arguments> scenarios() {
        Set<String> samuV3 = Set.of(SAMU1_V3, SAMU2_V3, SAMU_HYBRID);
        Set<String> transcodingCisu = Set.of(SAMU1_V3, SAMU2_V3);
        return Stream.of(
                Arguments.of(new SamuGpsScenario(), Set.of(SAMU1_V3, SAMU2_V3), Set.of(SAMU1_V3, SAMU2_V3)),
                Arguments.of(new SamuSmurScenario(), Set.of(SAMU1_V3, SAMU2_V3), Set.of(SAMU1_V3, SAMU2_V3)),
                Arguments.of(new SamuSamuDirectScenario(), samuV3, samuV3),
                Arguments.of(new SamuSamuConversionScenario(), samuV3, Set.of(SAMU1_V1)),
                Arguments.of(new SamuNexsisDirectScenario(), Set.of(SAMU_DIRECT_CISU, SAMU_HYBRID), Set.of(NEXSIS)),
                Arguments.of(new SamuNexsisConversionScenario(), transcodingCisu, Set.of(NEXSIS)),
                Arguments.of(new SamuNexsisRsScenario(), transcodingCisu, Set.of(NEXSIS)),
                Arguments.of(new SamuNexsisRcRiScenario(), Set.of(NEXSIS), transcodingCisu));
    }

    @ParameterizedTest
    @MethodSource("scenarios")
    void pickerDrawsFromScenarioConstraints(TmcScenario scenario, Set<String> senders, Set<String> recipients)
            throws Exception {
        ClientPropertiesRegistry registry = ClientPropertiesRegistry.load(
                Path.of(getClass().getClassLoader().getResource("clients/clients-scenarios.yaml").toURI()));

        List<ClientPair> pairs = scenario.clientPairPicker(registry).pairs();

        assertThat(pairs.stream().map(ClientPair::senderId).collect(Collectors.toSet())).isEqualTo(senders);
        assertThat(pairs.stream().map(ClientPair::recipientId).collect(Collectors.toSet())).isEqualTo(recipients);
    }

    @ParameterizedTest
    @MethodSource("scenarios")
    void buildFailsBeforeInjectionWithoutAvailablePair(TmcScenario scenario) throws Exception {
        ClientPropertiesRegistry registry = ClientPropertiesRegistry.load(Files.writeString(tempDir.resolve("clients.yaml"), """
                clients:
                  - client_id: fr.fire.nexsis.sdisZ
                    perimeters:
                      - name: 15-nexsis
                        versions: ["active"]
                """));

        assertThatThrownBy(() -> scenario.build(registry))
                .isInstanceOf(ClientConfigurationException.class)
                .hasMessageContaining("No sender/recipient pair available for scenario " + scenario.key());
    }
}
