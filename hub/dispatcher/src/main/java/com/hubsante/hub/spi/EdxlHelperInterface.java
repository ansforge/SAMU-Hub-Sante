package com.hubsante.hub.spi;

import com.hubsante.hub.spi.edxl.DistributionKind;

public interface EdxlHelperInterface {

    EdxlMessageInterface buildEdxlMessage(String UUID, String HUB_ID, String recipientId, long DEFAULT_HUB_MESSAGE_EXPIRATION, DistributionKind distributionKind, ContentMessageInterface contentMessage);

}
