package com.hubsante;

import com.hubsante.model.builders.DistributionElementBuilder;
import com.hubsante.model.builders.EDXL_DE_Builder;
import com.hubsante.model.builders.ReferenceWrapperBuilder;
import com.hubsante.model.edxl.DistributionKind;
import com.hubsante.model.edxl.EdxlMessage;
import com.hubsante.model.rcde.DistributionElement;
import com.hubsante.model.rcde.Recipient;
import com.hubsante.model.reference.ReferenceWrapper;


import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class Utils {

    /*
     * In the context of the code example, retrieves the client id thanks to the routing key passed as argument
     */
    public static String getClientId(String[] strings) {
        String[] routing = getRouting(strings).split("[.]");
        List<String> routingKey = Arrays.stream(routing).limit(routing.length-1).collect(Collectors.toList());
        return String.join(".", routingKey);
    }

    /*
     * In the context of the code example, retrieves the routing key passed as argument
     */
    public static String getRouting(String[] strings) {
        if (strings.length < 1)
            return "anonymous.info";
        return strings[0];
    }

    public static EdxlMessage referenceMessageFromReceivedMessage(EdxlMessage receivedMessage) {
        String referencedMessageRecipient = receivedMessage.getDescriptor().getExplicitAddress().getExplicitAddressValue();
        String referencedMessageSender = receivedMessage.getSenderID();
        String referencedMessageDistributionID = receivedMessage.getDistributionID();

        String distributionID = referencedMessageRecipient + "_" + UUID.randomUUID();

        Recipient recipient = new Recipient();
        recipient.setName(referencedMessageSender);
        recipient.setURI("hubex:" + referencedMessageSender);
        List<Recipient> recipients = new ArrayList<>();
        recipients.add(recipient);

        DistributionElement distributionElement =
                new DistributionElementBuilder(distributionID,referencedMessageSender, recipients)
                        .kind(DistributionElement.KindEnum.ACK).status(DistributionElement.StatusEnum.SYSTEM).build();
        ReferenceWrapper referenceWrapper =
                new ReferenceWrapperBuilder(distributionElement, referencedMessageDistributionID).build();

        return new EDXL_DE_Builder(distributionID, referencedMessageRecipient, referencedMessageSender)
                .distributionKind(DistributionKind.ACK)
                .distributionStatus(receivedMessage.getDistributionStatus())
                .contentMessage(referenceWrapper)
                .build();
    }

    public static String convertBytesToString(byte[] content){
        return new String(content, StandardCharsets.UTF_8);
    }
}
