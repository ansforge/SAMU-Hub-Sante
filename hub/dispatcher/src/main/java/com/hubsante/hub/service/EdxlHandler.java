package com.hubsante.hub.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.hubsante.model.edxl.ContentMessage;
import com.hubsante.model.edxl.EdxlMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EdxlHandler {

    @Qualifier("rootXmlMapper")
    @Autowired
    private XmlMapper xmlMapper;

    @Autowired
    private ObjectMapper jsonMapper;

    public EdxlHandler() {
    }

    public EdxlMessage deserializeJsonEDXL(String json) throws JsonProcessingException {
        return jsonMapper.readValue(json, EdxlMessage.class);
    }

    public ContentMessage deserializeJsonContentMessage(String json) throws JsonProcessingException {
        EdxlMessage edxlMessage = deserializeJsonEDXL(json);
        return edxlMessage.getContent().getContentObject().getContentWrapper()
                .getEmbeddedContent().getMessage();
    }

    public EdxlMessage deserializeXmlEDXL(String xml) throws JsonProcessingException {
        return xmlMapper.readValue(xml, EdxlMessage.class);
    }

    public ContentMessage deserializeXmlContentMessage(String xml) throws JsonProcessingException {
        EdxlMessage edxlMessage = deserializeXmlEDXL(xml);
        return edxlMessage.getContent().getContentObject().getContentWrapper()
                .getEmbeddedContent().getMessage();
    }

    public String serializeJsonEDXL(EdxlMessage edxlMessage) throws JsonProcessingException {
        return jsonMapper.writeValueAsString(edxlMessage);
    }

    public String prettyPrintJsonEDXL(EdxlMessage edxlMessage) throws JsonProcessingException {
        return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(edxlMessage);
    }

    public String serializeXmlEDXL(EdxlMessage edxlMessage) throws JsonProcessingException {
        return xmlMapper.writeValueAsString(edxlMessage);
    }

    public String prettyPrintXmlEDXL(EdxlMessage edxlMessage) throws JsonProcessingException {
        return xmlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(edxlMessage);
    }
}
