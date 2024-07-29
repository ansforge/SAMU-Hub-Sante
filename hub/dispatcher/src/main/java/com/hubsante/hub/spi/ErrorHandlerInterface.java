package com.hubsante.hub.spi;

import com.hubsante.hub.spi.report.ErrorCode;

public interface ErrorHandlerInterface {

    ErrorInterface createError(ErrorCode errorCode, String message);
}
