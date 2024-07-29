package com.hubsante.hub.spi;

import java.util.HashMap;
import java.util.Map;

public interface ErrorInterface {

    void setErrorCode(ErrorCode errorCode);

    void setSourceMessage (Map<String, Object> sourceMessage);

    void setReferencedDistributionID(String referencedDistributionID);

    getErrorCode()
}
