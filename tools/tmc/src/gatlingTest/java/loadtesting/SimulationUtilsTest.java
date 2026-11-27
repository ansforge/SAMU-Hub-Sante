package loadtesting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubsante.model.Utils;
import loadtesting.clients.ClientPair;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static loadtesting.Constants.GATLING_EL_SENDER_ID_KEY;
import static loadtesting.Constants.SENDER_ID_ROUTING_KEY;
import static org.assertj.core.api.Assertions.assertThat;

class SimulationUtilsTest {
    private static final ObjectMapper jsonMapper = Utils.getJsonMapper();

    private static final ClientPair SAMU1_TO_NEXSIS = new ClientPair("fr.health.test.samu1-v3", "fr.fire.nexsis.sdisZ");
    private static final ClientPair SAMU2_TO_NEXSIS = new ClientPair("fr.health.test.samu2-v3", "fr.fire.nexsis.sdisZ");

    @Test
    void senderIdRoutingKeyTargetsSenderIdFeederKey() {
        assertThat(SENDER_ID_ROUTING_KEY).isEqualTo("#{" + GATLING_EL_SENDER_ID_KEY + "}");
    }

    @Test
    void messageFeederUsesSuppliedPairForEachMessage() throws Exception {
        Iterator<Map<String, Object>> feeder = SimulationUtils.generateMessageFeeder(
                SimulationUtils.loadSampleFile("rs-eda.json"), alternating(SAMU1_TO_NEXSIS, SAMU2_TO_NEXSIS));

        for (ClientPair expected : List.of(SAMU1_TO_NEXSIS, SAMU2_TO_NEXSIS, SAMU1_TO_NEXSIS)) {
            Map<String, Object> entry = feeder.next();

            assertThat(pairOf(entry)).isEqualTo(expected);
            assertAddressedTo(entry.get("message"), expected);
        }
    }

    @Test
    void rsFlowFeederUsesSamePairAndCaseIdForBothMessages() throws Exception {
        Iterator<Map<String, Object>> feeder = SimulationUtils.generateRsFlowFeeder(
                SimulationUtils.loadSampleFile("rs-ri.json"), SimulationUtils.loadSampleFile("rs-sr.json"),
                alternating(SAMU1_TO_NEXSIS, SAMU2_TO_NEXSIS));

        for (ClientPair expected : List.of(SAMU1_TO_NEXSIS, SAMU2_TO_NEXSIS)) {
            Map<String, Object> entry = feeder.next();

            assertThat(pairOf(entry)).isEqualTo(expected);
            assertAddressedTo(entry.get("rsRiMessage"), expected);
            assertAddressedTo(entry.get("rsSrMessage"), expected);
            String caseId = caseIdOf(entry.get("rsRiMessage"));
            assertThat(caseId).startsWith(expected.senderId() + "_");
            assertThat(caseIdOf(entry.get("rsSrMessage"))).isEqualTo(caseId);
        }
    }

    @Test
    void rcRiFlowFeederUsesSamePairAndCaseIdForBothMessages() throws Exception {
        ClientPair nexsisToSamu1 = new ClientPair("fr.fire.nexsis.sdisZ", "fr.health.test.samu1-v3");
        ClientPair nexsisToSamu2 = new ClientPair("fr.fire.nexsis.sdisZ", "fr.health.test.samu2-v3");
        Iterator<Map<String, Object>> feeder = SimulationUtils.generateRcRiFlowFeeder(
                SimulationUtils.loadSampleFile("rc-ri.json"), alternating(nexsisToSamu1, nexsisToSamu2));

        for (ClientPair expected : List.of(nexsisToSamu1, nexsisToSamu2)) {
            Map<String, Object> entry = feeder.next();

            assertThat(pairOf(entry)).isEqualTo(expected);
            assertAddressedTo(entry.get("rcRiNewMessage"), expected);
            assertAddressedTo(entry.get("rcRiUpdateMessage"), expected);
            String caseId = caseIdOf(entry.get("rcRiNewMessage"));
            assertThat(caseId).startsWith(expected.senderId() + "_");
            assertThat(caseIdOf(entry.get("rcRiUpdateMessage"))).isEqualTo(caseId);
        }
    }

    private static Supplier<ClientPair> alternating(ClientPair first, ClientPair second) {
        Iterator<ClientPair> pairs = Stream.iterate(first, pair -> pair == first ? second : first).iterator();
        return pairs::next;
    }

    private static ClientPair pairOf(Map<String, Object> entry) {
        return new ClientPair((String) entry.get("senderId"), (String) entry.get("recipientId"));
    }

    private static void assertAddressedTo(Object message, ClientPair pair) throws Exception {
        JsonNode edxl = jsonMapper.readTree((String) message);
        assertThat(edxl.get("senderID").asText()).isEqualTo(pair.senderId());
        assertThat(edxl.get("distributionID").asText()).startsWith(pair.senderId() + "_");
        assertThat(edxl.at("/descriptor/explicitAddress/explicitAddressValue").asText()).isEqualTo(pair.recipientId());
        JsonNode distributionElement = edxl.at("/content/0/jsonContent/embeddedJsonContent/message");
        assertThat(distributionElement.at("/sender/URI").asText()).isEqualTo("hubex:" + pair.senderId());
        assertThat(distributionElement.at("/recipient/0/URI").asText()).isEqualTo("hubex:" + pair.recipientId());
    }

    private static String caseIdOf(Object message) throws Exception {
        return jsonMapper.readTree((String) message)
                .at("/content/0/jsonContent/embeddedJsonContent/message")
                .findValue("caseId").asText();
    }
}
