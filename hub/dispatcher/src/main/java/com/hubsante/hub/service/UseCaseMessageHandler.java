package com.hubsante.hub.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.hubsante.model.edxl.EdxlInnerMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UseCaseMessageHandler {

    @Autowired
    private ObjectMapper jsonMapper;

    @Autowired
    @Qualifier("xmlMapper")
    private XmlMapper xmlMapper;

    public String serializeJsonMessage(EdxlInnerMessage message) throws JsonProcessingException {
        return jsonMapper.writeValueAsString(message);
    }

    public String serializeXmlMessage(EdxlInnerMessage message) throws JsonProcessingException {
        return xmlMapper.writeValueAsString(message);
    }
}
