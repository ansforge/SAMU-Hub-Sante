/**
 * Copyright © 2023-2025 Agence du Numerique en Sante (ANS)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
