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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static loadtesting.Constants.RC_DE_USER_PREFIX;
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

    public static String buildEdxlMessageString(String useCaseString, String senderId, String recipientId, DistributionKind distributionKind, DistributionStatus distributionStatus) throws Exception {
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

    public static String buildEdxlMessageString(String useCaseString, String senderId, String recipientId, DistributionKind distributionKind) throws Exception {
        return buildEdxlMessageString(useCaseString, senderId, recipientId, distributionKind, DistributionStatus.ACTUAL);
    }

    public static String buildEdxlMessageString(String useCaseString, String senderId, String recipientId) throws Exception {
        return buildEdxlMessageString(useCaseString, senderId, recipientId, DistributionKind.REPORT, DistributionStatus.ACTUAL);
    }

    public static Iterator<Map<String, Object>> generateMessageFeeder(String useCaseString, String senderId, String recipientId) {
        return Stream.generate((Supplier<Map<String, Object>>) () ->
        {
            try {
                return Collections.singletonMap("message", buildEdxlMessageString(useCaseString, senderId, recipientId));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).iterator();
    }

    /**
     * Feeder that replaces the caseId field inside the use-case JSON with a fresh
     * UUID-based value on every generated message, ensuring no two messages share the same
     * caseId.
     */
    public static Iterator<Map<String, Object>> generateUniqueIdMessageFeeder(
            String useCaseString, String senderId, String recipientId) {
        return Stream.generate((Supplier<Map<String, Object>>) () ->
        {
            try {
                String uniqueCaseId = String.format("%s_%s", senderId, UUID.randomUUID());
                ObjectNode useCaseNode = (ObjectNode) jsonMapper.readTree(useCaseString);
                // The caseId is always one level deep inside the root use-case object
                // e.g. { "resourcesInfoCisu": { "caseId": "...", ... } }
                useCaseNode.fields().forEachRemaining(entry -> {
                    JsonNode inner = entry.getValue();
                    if (inner.isObject() && inner.has("caseId")) {
                        ((ObjectNode) inner).put("caseId", uniqueCaseId);
                    }
                });
                return Collections.singletonMap("message",
                        buildEdxlMessageString(jsonMapper.writeValueAsString(useCaseNode), senderId, recipientId));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).iterator();
    }

    /**
     * Feeder that replaces the caseId with each entry of caseIdPool exactly once,
     * in order. Used for the warmup phase to seed MongoDB with one document per caseId.
     */
    public static Iterator<Map<String, Object>> generateFixedPoolMessageFeeder(
            String useCaseString, String senderId, String recipientId, List<String> caseIdPool) {
        return caseIdPool.stream().map(caseId -> {
            try {
                ObjectNode useCaseNode = (ObjectNode) jsonMapper.readTree(useCaseString);
                useCaseNode.fields().forEachRemaining(entry -> {
                    JsonNode inner = entry.getValue();
                    if (inner.isObject() && inner.has("caseId")) {
                        ((ObjectNode) inner).put("caseId", caseId);
                    }
                });
                return Collections.<String, Object>singletonMap("message",
                        buildEdxlMessageString(jsonMapper.writeValueAsString(useCaseNode), senderId, recipientId));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).iterator();
    }

    /**
     * Feeder that cycles indefinitely over caseIdPool in round-robin. Used for the load
     * phase where every caseId is already known to MongoDB,
     * forcing the Converter to take the "known case" (DB lookup + diff) path.
     */
    public static Iterator<Map<String, Object>> generateRoundRobinMessageFeeder(
            String useCaseString, String senderId, String recipientId, List<String> caseIdPool) {
        AtomicInteger index = new AtomicInteger(0);
        return Stream.generate((Supplier<Map<String, Object>>) () ->
        {
            try {
                String caseId = caseIdPool.get(index.getAndIncrement() % caseIdPool.size());
                ObjectNode useCaseNode = (ObjectNode) jsonMapper.readTree(useCaseString);
                useCaseNode.fields().forEachRemaining(entry -> {
                    JsonNode inner = entry.getValue();
                    if (inner.isObject() && inner.has("caseId")) {
                        ((ObjectNode) inner).put("caseId", caseId);
                    }
                });
                return Collections.singletonMap("message",
                        buildEdxlMessageString(jsonMapper.writeValueAsString(useCaseNode), senderId, recipientId));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).iterator();
    }
}