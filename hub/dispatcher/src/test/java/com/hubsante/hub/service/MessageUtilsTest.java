package com.hubsante.hub.service;

import com.hubsante.model.builders.EDXL_DE_Builder;
import com.hubsante.model.edxl.EdxlMessage;
import org.junit.jupiter.api.Test;

import static com.hubsante.hub.utils.ConversionUtils.isTargetPerimeter;
import static com.hubsante.hub.utils.ConversionUtils.isTranscodingVersion;
import static com.hubsante.hub.utils.MessageUtils.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MessageUtilsTest {

    @Test
    public void isExternalHubexInvolvedTest() {
        EdxlMessage incomingHubexEdxl = new EDXL_DE_Builder("uuid_1", "fr.fire.something", "fr.health.something").build();
        EdxlMessage outgoingHubexEdxl = new EDXL_DE_Builder("uuid_2", "fr.health.something", "fr.fire.something").build();
        EdxlMessage internalEdxl = new EDXL_DE_Builder("uuid_3", "fr.health.something", "fr.health.something-else").build();

        assertTrue(isExternalHubexInvolved(incomingHubexEdxl));
        assertTrue(isExternalHubexInvolved(outgoingHubexEdxl));
        assertFalse(isExternalHubexInvolved(internalEdxl));
    }
}
