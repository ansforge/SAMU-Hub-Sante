package com.hubsante.hub.spi;

import com.hubsante.hub.spi.edxl.DistributionKind;

import java.time.OffsetDateTime;

public interface EdxlMessageInterface {

    DistributionKind getDistributionKind();

    DescriptorInterface getDescriptor();

    String getSenderID();

    String getDistributionID();

    OffsetDateTime getDateTimeExpires();

    Object getFirstContentMessage();

    void setDateTimeSent(OffsetDateTime now);

    void setDateTimeExpires(OffsetDateTime offsetDateTime);
}
