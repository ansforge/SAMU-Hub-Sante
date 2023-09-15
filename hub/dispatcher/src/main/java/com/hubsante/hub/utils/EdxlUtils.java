package com.hubsante.hub.utils;

import com.hubsante.model.edxl.*;

import java.time.OffsetDateTime;
import java.util.UUID;

public class EdxlUtils {
    private static final String HUB_ID = "fr.health.hub";
    private static final String HUBEX_SCHEME = "hubex";
    private static final long DEFAULT_ERROR_EXPIRATION = 1L;
    private static final String DEFAULT_HUB_LANGUAGE = "fr-FR";

    public static EdxlMessage edxlMessageFromHub(String recipientId, ContentMessage contentMessage) {
        OffsetDateTime now = OffsetDateTime.now();
        return new EdxlMessage(
                UUID(),
                HUBSANTE_ID,
                now,
                dateTimesExpires(now),
                DistributionStatus.ACTUAL,
                DistributionKind.ERROR,
                descriptor(recipientId),
                contentMessage
        );
    }

    private static String UUID() {
        return HUBSANTE_ID + "_" + UUID.randomUUID();
    }

    private static OffsetDateTime dateTimesExpires(OffsetDateTime dateTimeSent) {
        return dateTimeSent.plusDays(DEFAULT_ERROR_EXPIRATION);
    }

    private static Descriptor descriptor(String recipientId) {
        ExplicitAddress explicitAddress = new ExplicitAddress(HUBSANTE_SCHEME, recipientId);
        return new Descriptor(DEFAULT_HUB_LANGUAGE, explicitAddress);
    }
}
