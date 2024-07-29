package com.hubsante.hub.spi;

import com.hubsante.hub.spi.exception.ValidationException;

public interface ValidatorInterface {
    void validateJSON(String receivedEdxl, String envelopeSchema) throws ValidationException;

    void validateXML(String receivedEdxl, String envelopeXsd) throws ValidationException;
}
