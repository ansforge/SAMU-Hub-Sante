package com.hubsante.hub.utils;

import com.hubsante.model.edxl.EdxlMessage;

import static com.hubsante.hub.utils.MessageUtils.getRecipientID;

public class ConversionUtils {
    public static boolean isCisuExchange(EdxlMessage edxlMessage) {
        String recipientID = getRecipientID(edxlMessage);
        String senderID = edxlMessage.getSenderID();
        return !(recipientID.startsWith("fr.health") && senderID.startsWith("fr.health"));
    }
}
