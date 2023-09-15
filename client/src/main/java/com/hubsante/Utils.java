package com.hubsante;

import com.hubsante.model.cisu.*;
import com.hubsante.model.edxl.Descriptor;
import com.hubsante.model.edxl.DistributionKind;
import com.hubsante.model.edxl.EdxlMessage;
import com.hubsante.model.edxl.ExplicitAddress;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class Utils {

    /*
    * Dans le contexte de ce code exemple, cette méthode permet de récupérer un clientId à partir
    * de la routing key fournie en argument de run
     */
    static String getClientId(String[] strings) {
        String[] routing = getRouting(strings).split("[.]");
        List<String> routingKey = Arrays.stream(routing).limit(routing.length-1).collect(Collectors.toList());
        return String.join(".", routingKey);
    }

    /*
    * Dans le contexte de ce code exemple, cette méthode permet de récupérer la routing key
    * passée en argument de run
     */
    static String getRouting(String[] strings) {
        if (strings.length < 1)
            return "anonymous.info";
        return strings[0];
    }

    static EdxlMessage referenceMessageFromReceivedMessage(EdxlMessage receivedMessage) {
        String referencedMessageRecipient = receivedMessage.getDescriptor().getExplicitAddress().getExplicitAddressValue();
        String referencedMessageSender = receivedMessage.getSenderID();
        String referencedMessageDistributionID = receivedMessage.getDistributionID();

        String distributionID = referencedMessageRecipient + "_" + UUID.randomUUID();

        Sender sender = new Sender();
        sender.setName(referencedMessageRecipient);
        sender.setURI("hubex:" + referencedMessageRecipient);

        Recipient recipient = new Recipient();
        recipient.setName(referencedMessageSender);
        recipient.setURI("hubex:" + referencedMessageSender);
        List<Recipient> recipients = new ArrayList<>();
        recipients.add(recipient);

        OffsetDateTime dateTimeSent = OffsetDateTime.of(LocalDateTime.now(), ZoneOffset.of("+02"));
        OffsetDateTime dateTimeExpires = dateTimeSent.plusYears(50);

        Reference reference = new Reference();
        reference.setDistributionID(referencedMessageDistributionID);

        ReferenceMessage referenceMessage = new ReferenceMessage();
        referenceMessage.setMessageId(distributionID);
        referenceMessage.setSender(sender);
        referenceMessage.setSentAt(dateTimeSent);
        referenceMessage.setKind(DistributionElement.KindEnum.ACK);
        referenceMessage.setStatus(DistributionElement.StatusEnum.SYSTEM);
        referenceMessage.setRecipients(recipients);
        referenceMessage.setReference(reference);

        ExplicitAddress explicitAddress = new ExplicitAddress();
        explicitAddress.setExplicitAddressScheme(receivedMessage.getSenderID());
        explicitAddress.setExplicitAddressValue(receivedMessage.getSenderID());

        Descriptor descriptor = new Descriptor();
        descriptor.setLanguage(receivedMessage.getDescriptor().getLanguage());
        descriptor.setExplicitAddress(explicitAddress);

        return new EdxlMessage(
                distributionID,
                referencedMessageRecipient,
                dateTimeSent,
                dateTimeExpires,
                receivedMessage.getDistributionStatus(),
                DistributionKind.ACK,
                descriptor,
                referenceMessage
        );
    }
}
