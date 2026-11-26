package loadtesting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hubsante.model.Utils;
import com.hubsante.model.builders.EDXL_DE_Builder;
import com.hubsante.model.edxl.*;
import com.hubsante.model.rcde.DistributionElement;
import com.hubsante.model.rcde.Recipient;
import com.hubsante.model.rcde.Sender;
import loadtesting.clients.ClientPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static loadtesting.Constants.*;
import static loadtesting.ConfigUtils.getNumericEnvVar;

public final class SimulationUtils {
    private static final Logger log = LoggerFactory.getLogger(SimulationUtils.class);
    private static final ObjectMapper jsonMapper = Utils.getJsonMapper();

    public static String loadSampleFile(String fileName) throws Exception {
        InputStream fileStream = SimulationUtils.class.getClassLoader().getResourceAsStream("messages/" + fileName);
        if (fileStream == null) throw new IOException("Resource not found:" + fileName);
        return new String(fileStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    private static String sanitizeClientId(String clientId) {
        if (clientId == null) return null;
        String[] parts = clientId.split("\\.");
        if (parts.length <= 2) return "";
        return String.join(".", Arrays.copyOfRange(parts, 2, parts.length));
    }

    public static String buildEdxlMessageString(String useCaseString, ClientPair pair, DistributionKind distributionKind, DistributionStatus distributionStatus) throws Exception {
        String senderId = pair.senderId();
        String recipientId = pair.recipientId();

        // The JSON mapper config used in the library forces in the serialization
        // of the dates to be truncated to seconds.
        OffsetDateTime sentAt = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        String distributionId = String.format("%s_%s", senderId, UUID.randomUUID());

        EdxlMessage edxlMessage = new EDXL_DE_Builder(distributionId, senderId, recipientId)
                .dateTimeSent(sentAt)
                .distributionKind(distributionKind)
                .distributionStatus(distributionStatus)
                .build();

        String sanitizedSenderId = sanitizeClientId(senderId);
        String sanitizedRecipientId = sanitizeClientId(recipientId);

        Sender sender = new Sender().name(sanitizedSenderId).URI(String.format("%s:%s", RC_DE_USER_PREFIX, senderId));
        Recipient recipient = new Recipient().name(sanitizedRecipientId).URI(String.format("%s:%s", RC_DE_USER_PREFIX, recipientId));
        List<Recipient> recipients = new ArrayList<>();
        recipients.add(recipient);

        DistributionElement messageDistributionElement = new DistributionElement();
        messageDistributionElement.setRecipient(recipients);
        messageDistributionElement.setSender(sender);
        messageDistributionElement.setMessageId(distributionId);
        messageDistributionElement.setSentAt(sentAt);


        ObjectNode edxlJson = jsonMapper.valueToTree(edxlMessage);
        JsonNode useCaseJson = jsonMapper.readTree(useCaseString);

        ObjectNode distributionElementJson = jsonMapper.valueToTree(messageDistributionElement);
        distributionElementJson.setAll((ObjectNode) useCaseJson);
        // Set the distribution kind and status here as string because the kind and status are different enums
        // in RC-DE and EDXL (but have the same string values behind)
        distributionElementJson
                .put("status", distributionStatus.getValue())
                .put("kind", distributionKind.getValue());

        ((ObjectNode) edxlJson.get("content").get(0).get("jsonContent").get("embeddedJsonContent")).put("message", distributionElementJson);

        return jsonMapper.writeValueAsString(edxlJson);
    }

    public static String buildEdxlMessageString(String useCaseString, ClientPair pair, DistributionKind distributionKind) throws Exception {
        return buildEdxlMessageString(useCaseString, pair, distributionKind, DistributionStatus.ACTUAL);
    }

    public static String buildEdxlMessageString(String useCaseString, ClientPair pair) throws Exception {
        return buildEdxlMessageString(useCaseString, pair, DistributionKind.REPORT, DistributionStatus.ACTUAL);
    }

    public static Iterator<Map<String, Object>> generateMessageFeeder(String useCaseString, Supplier<ClientPair> pairs) {
        return Stream.generate((Supplier<Map<String, Object>>) () ->
        {
            try {
                ClientPair pair = pairs.get();
                return feederEntry(pair, Map.of(GATLING_EL_MESSAGE_KEY, buildEdxlMessageString(useCaseString, pair)));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).iterator();
    }

    /** Feeder for the 15-18 flow: generates an RS-RI and RS-SR sharing the same unique caseId per virtual user. */
    public static Iterator<Map<String, Object>> generateRsFlowFeeder(
            String rsRiContent, String rsSrContent, Supplier<ClientPair> pairs) {
        return Stream.generate(() -> {
            try {
                ClientPair pair = pairs.get();
                String caseId = pair.senderId() + "_" + UUID.randomUUID();
                String rsRiMessage = buildEdxlMessageString(
                        jsonMapper.writeValueAsString(replaceCaseId(rsRiContent, caseId)), pair);
                String rsSrMessage = buildEdxlMessageString(
                        jsonMapper.writeValueAsString(replaceCaseId(rsSrContent, caseId)), pair);
                return feederEntry(pair, Map.of("rsRiMessage", rsRiMessage, "rsSrMessage", rsSrMessage));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).iterator();
    }

    /** Feeder for the 18-15 flow: generates two RC-RI messages sharing the same unique caseId per virtual user.
     * The first triggers the new-case path; the second (sent after a pause) triggers the known-case diff path. */
    public static Iterator<Map<String, Object>> generateRcRiFlowFeeder(
            String rcRiContent, Supplier<ClientPair> pairs) {
        return Stream.generate(() -> {
            try {
                ClientPair pair = pairs.get();
                String caseId = pair.senderId() + "_" + UUID.randomUUID();
                String newCaseMessage = buildEdxlMessageString(
                        jsonMapper.writeValueAsString(replaceCaseId(rcRiContent, caseId)), pair);
                String updateMessage = buildEdxlMessageString(
                        jsonMapper.writeValueAsString(replaceCaseId(rcRiContent, caseId)), pair);
                return feederEntry(pair, Map.of("rcRiNewMessage", newCaseMessage, "rcRiUpdateMessage", updateMessage));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).iterator();
    }

    private static Map<String, Object> feederEntry(ClientPair pair, Map<String, Object> messages) {
        Map<String, Object> entry = new HashMap<>(messages);
        entry.put(GATLING_EL_SENDER_ID_KEY, pair.senderId());
        entry.put(GATLING_EL_RECIPIENT_ID_KEY, pair.recipientId());
        return entry;
    }

    private static ObjectNode replaceCaseId(String useCaseString, String caseId) throws Exception {
        ObjectNode useCaseNode = (ObjectNode) jsonMapper.readTree(useCaseString);
        useCaseNode.fields().forEachRemaining(entry -> {
            JsonNode inner = entry.getValue();
            if (inner.isObject() && inner.has("caseId")) {
                ((ObjectNode) inner).put("caseId", caseId);
            }
        });
        return useCaseNode;
    }
}