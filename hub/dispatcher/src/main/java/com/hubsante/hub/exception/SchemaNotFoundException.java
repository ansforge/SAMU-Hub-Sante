package com.hubsante.hub.exception;

import com.hubsante.model.report.ErrorCode;

public class SchemaNotFoundException extends AbstractHubException{

    public SchemaNotFoundException(String message, String referencedDistributionID) {
        super(message, ErrorCode.SCHEMA_NOT_FOUND, referencedDistributionID); }
}
