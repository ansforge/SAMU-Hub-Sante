package com.hubsante;

import com.hubsante.model.builders.EDXL_DE_Builder;
import com.hubsante.model.custom.CustomMessage;
import com.hubsante.model.edxl.DistributionKind;
import com.hubsante.model.edxl.EdxlMessage;
import com.hubsante.model.reference.Reference;
import com.hubsante.model.reference.ReferenceWrapper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UtilsTest {
    private static String[] argv = {"fr.health.hub.samu001.message", "message", "content"};

    @Test
    public void getRouting() {
        assertEquals(Utils.getRouting(argv), "fr.health.hub.samu001.message");
    }

    @Test
    public void getClientId() {
        assertEquals(Utils.getClientId(argv), "fr.health.hub.samu001");
    }

    @Test
    public void buildReferenceMessage() {
        CustomMessage message = new CustomMessage();
        EdxlMessage edxlMessage = new EDXL_DE_Builder(
                "sender-x_1234", "sender-x", "recipient-y")
                .contentMessage(message)
                .build();

        EdxlMessage ack = Utils.referenceMessageFromReceivedMessage(edxlMessage);

        assertTrue(ack.getDistributionID().startsWith("recipient-y_"));
        assertEquals("recipient-y", ack.getSenderID());
        assertEquals(DistributionKind.ACK, ack.getDistributionKind());

        Reference reference = ((ReferenceWrapper) ack.getFirstContentMessage()).getReference();
        assertEquals("sender-x_1234", reference.getDistributionID());
    }
}