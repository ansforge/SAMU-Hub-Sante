package com.hubsante.hub.utils;

import com.hubsante.model.builders.EDXL_DE_Builder;
import com.hubsante.model.edxl.*;

import java.time.OffsetDateTime;
import java.util.UUID;

public class EdxlUtils {
    public static final String HUB_ID = "fr.health.hub";
    private static final long DEFAULT_HUB_MESSAGE_EXPIRATION = 1L;

    public static EdxlMessage edxlMessageFromHub(String recipientId, ContentMessage contentMessage) {
        return new EDXL_DE_Builder(UUID(),HUB_ID, recipientId)
                .dateTimeSentNowWithOffsetInDays(DEFAULT_HUB_MESSAGE_EXPIRATION)
                .distributionKind(DistributionKind.ERROR)
                .contentMessage(contentMessage)
                .build();
    }

    private static String UUID() {
        return HUB_ID + "_" + UUID.randomUUID();
    }
}
