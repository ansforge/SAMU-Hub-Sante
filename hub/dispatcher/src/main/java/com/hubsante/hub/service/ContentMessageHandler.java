package com.hubsante.hub.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.hubsante.model.cisu.CreateCase;
import com.hubsante.model.cisu.CreateCaseMessage;
import com.hubsante.model.edxl.ContentMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ContentMessageHandler {

    @Autowired
    private ObjectMapper jsonMapper;

    @Autowired
    @Qualifier("xmlMapper")
    private XmlMapper xmlMapper;

    public String serializeJsonMessage(ContentMessage message) throws JsonProcessingException {
        return jsonMapper.writeValueAsString(message);
    }

    public String serializeJsonCreateCase(CreateCaseMessage message) throws JsonProcessingException {
        CreateCase createCase = message.getCreateCase();
        return jsonMapper.writeValueAsString(createCase);
    }

    public String serializeXmlMessage(ContentMessage message) throws JsonProcessingException {
        return xmlMapper.writeValueAsString(message);
    }

    public ContentMessage deserializeJsonMessage(String json) throws JsonProcessingException {
        return jsonMapper.readValue(json, ContentMessage.class);
    }

    public ContentMessage deserializeXmlMessage(String xml) throws JsonProcessingException {
        return xmlMapper.readValue(xml, ContentMessage.class);
    }

}
