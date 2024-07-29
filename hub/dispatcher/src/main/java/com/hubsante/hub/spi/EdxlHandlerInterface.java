package com.hubsante.hub.spi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.jknack.handlebars.internal.lang3.StringUtils;

public interface EdxlHandlerInterface {

    EdxlMessageInterface deserializeJsonEDXL(String jsonEDXL) throws JsonProcessingException;

    String serializeXmlEDXL(EdxlMessageInterface errorEdxlMessage) throws JsonProcessingException;

    String serializeJsonEDXL(EdxlMessageInterface errorEdxlMessage) throws JsonProcessingException;

    EdxlEnvelopeInterface deserializeJsonEDXLEnvelope(String receivedEdxl) throws JsonProcessingException;

    EdxlEnvelopeInterface deserializeXmlEDXLEnvelope(String receivedEdxl) throws JsonProcessingException;

    EdxlMessageInterface deserializeXmlEDXL(String receivedEdxl) throws JsonProcessingException;
}
