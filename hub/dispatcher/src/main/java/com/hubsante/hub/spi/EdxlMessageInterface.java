package com.hubsante.hub.spi;

import com.hubsante.hub.spi.edxl.DistributionKind;

import java.time.OffsetDateTime;

public interface EdxlMessageInterface {

    String getSenderID();

    String getDistributionID();

    OffsetDateTime getDateTimeExpires();

    void setDateTimeSent(OffsetDateTime now);

    void setDateTimeExpires(OffsetDateTime offsetDateTime);
}
