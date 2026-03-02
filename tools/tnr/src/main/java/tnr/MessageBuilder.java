package tnr;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static tnr.Constants.RC_DE_USER_PREFIX;
import static tnr.Utils.sanitizeClientId;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hubsante.model.Utils;
import com.hubsante.model.builders.EDXL_DE_Builder;
import com.hubsante.model.edxl.DistributionKind;
import com.hubsante.model.edxl.DistributionStatus;
import com.hubsante.model.edxl.EdxlMessage;
import com.hubsante.model.rcde.DistributionElement;
import com.hubsante.model.rcde.Recipient;
import com.hubsante.model.rcde.Sender;

public class MessageBuilder {

    private static final ObjectMapper JSON_MAPPER;

    static {
        JSON_MAPPER = Utils.getJsonMapper()
                .registerModule(new JavaTimeModule())
                .findAndRegisterModules();
    }

    public String buildMessage(
            String useCaseString,
            String distributionId,
            String senderId,
            String recipientId
    ) throws JsonProcessingException {
        return buildMessage(useCaseString, distributionId, senderId, recipientId,
                DistributionKind.REPORT, DistributionStatus.ACTUAL);
    }

    public String buildMessage(
            String useCaseString,
            String distributionId,
            String senderId,
            String recipientId,
            DistributionKind distributionKind,
            DistributionStatus distributionStatus
    ) throws JsonProcessingException {

        OffsetDateTime sentAt = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        EdxlMessage edxlMessage = new EDXL_DE_Builder(distributionId, senderId, recipientId)
                .dateTimeSent(sentAt)
                .distributionKind(distributionKind)
                .distributionStatus(distributionStatus)
                .build();

        DistributionElement distributionElement = buildDistributionElement(
                distributionId,
                senderId,
                recipientId,
                sentAt
        );

        ObjectNode edxlJson = JSON_MAPPER.valueToTree(edxlMessage);
        JsonNode useCaseJson = JSON_MAPPER.readTree(useCaseString);

        ObjectNode distributionElementJson = JSON_MAPPER.valueToTree(distributionElement);
        distributionElementJson.setAll((ObjectNode) useCaseJson);

        distributionElementJson
                .put("status", distributionStatus.getValue())
                .put("kind", distributionKind.getValue());

        ObjectNode embeddedNode = (ObjectNode) edxlJson
                .path("content")
                .path(0)
                .path("jsonContent")
                .path("embeddedJsonContent");

        embeddedNode.set("message", distributionElementJson);

        return JSON_MAPPER.writeValueAsString(edxlJson);
    }

    private DistributionElement buildDistributionElement(
            String distributionId,
            String senderId,
            String recipientId,
            OffsetDateTime sentAt
    ) {

        String sanitizedSenderId = sanitizeClientId(senderId);
        String sanitizedRecipientId = sanitizeClientId(recipientId);

        Sender sender = new Sender()
                .name(sanitizedSenderId)
                .URI(RC_DE_USER_PREFIX + ":" + senderId);

        Recipient recipient = new Recipient()
                .name(sanitizedRecipientId)
                .URI(RC_DE_USER_PREFIX + ":" + recipientId);

        List<Recipient> recipients = new ArrayList<>();
        recipients.add(recipient);

        DistributionElement element = new DistributionElement();
        element.setRecipient(recipients);
        element.setSender(sender);
        element.setMessageId(distributionId);
        element.setSentAt(sentAt);

        return element;
    }
}
