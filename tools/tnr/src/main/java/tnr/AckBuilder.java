package tnr;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.hubsante.model.Utils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hubsante.model.builders.DistributionElementBuilder;
import com.hubsante.model.builders.EDXL_DE_Builder;
import com.hubsante.model.builders.ReferenceWrapperBuilder;
import com.hubsante.model.edxl.DistributionKind;
import com.hubsante.model.edxl.EdxlMessage;
import com.hubsante.model.rcde.DistributionElement;
import com.hubsante.model.rcde.Recipient;
import com.hubsante.model.reference.ReferenceWrapper;

public class AckBuilder {

    private static final ObjectMapper JSON_MAPPER;

    static {
        JSON_MAPPER = Utils.getJsonMapper()
                .registerModule(new JavaTimeModule())
                .findAndRegisterModules();
    }

    String buildAck(String senderId, String recipientId, String ackDistributionId, String referencedDistributionId) throws JsonProcessingException {

        Recipient recipient = new Recipient().name(senderId).URI("hubex:" + senderId);
        List<Recipient> recipientList = Stream.of(recipient).collect(Collectors.toList());

        DistributionElement distributionElement = new DistributionElementBuilder(ackDistributionId, senderId, recipientList)
                .kind(DistributionElement.KindEnum.ACK)
                .build();
        ReferenceWrapper referenceWrapper = new ReferenceWrapperBuilder(distributionElement, referencedDistributionId)
                .build();

        EdxlMessage built = new EDXL_DE_Builder(ackDistributionId, senderId, recipientId)
                .contentMessage(referenceWrapper)
                .distributionKind(DistributionKind.ACK)
                .build();
        ObjectNode edxlJson = JSON_MAPPER.valueToTree(built);
        return JSON_MAPPER.writeValueAsString(edxlJson);
    }
}
