package com.hubsante.hub.spi;

import com.hubsante.hub.spi.report.Error;
import com.hubsante.hub.spi.report.ErrorCode;

public interface ErrorHandlerInterface {

    Error createError(ErrorCode errorCode, String message);
}
