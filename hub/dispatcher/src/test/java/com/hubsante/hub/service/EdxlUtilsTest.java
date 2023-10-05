package com.hubsante.hub.service;

import com.hubsante.hub.utils.EdxlUtils;
import com.hubsante.model.edxl.DistributionKind;
import com.hubsante.model.edxl.DistributionStatus;
import com.hubsante.model.edxl.EdxlMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EdxlUtilsTest {

    @Test
    public void testEdxlMessageFromHub() {
        String recipientId = "fr.health.samu123";

        EdxlMessage edxlMessage = EdxlUtils.edxlMessageFromHub(recipientId, null);

        assertTrue(edxlMessage.getDistributionID().startsWith("fr.health.hub_"));
        assertEquals(edxlMessage.getSenderID(), "fr.health.hub");
        assertEquals(edxlMessage.getDateTimeSent().plusDays(1), edxlMessage.getDateTimeExpires());
        assertEquals(DistributionStatus.ACTUAL, edxlMessage.getDistributionStatus());
        assertEquals(DistributionKind.ERROR, edxlMessage.getDistributionKind());
        assertEquals("fr-FR", edxlMessage.getDescriptor().getLanguage());
        assertEquals("hubex", edxlMessage.getDescriptor().getExplicitAddress().getExplicitAddressScheme());
        assertEquals(recipientId, edxlMessage.getDescriptor().getExplicitAddress().getExplicitAddressValue());
    }
}
