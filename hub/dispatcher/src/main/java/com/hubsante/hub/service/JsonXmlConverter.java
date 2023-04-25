package com.hubsante.hub.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import com.hubsante.message.CreateEventMessage;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;

@Service
public class JsonXmlConverter {

    /**
     * SerializationFeature.WRITE_DATES_AS_TIMESTAMPS & DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE
     * must be disabled to preserve offset through deserialization
     * --> see https://stackoverflow.com/questions/40488002/how-to-preserve-the-offset-while-deserializing-offsetdatetime-with-jackson
     * <p>
     * MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES is set to true to handle deserialization with Jackson when xml element & java props
     * have not the same case
     * When we will use OpenAPI generator the Java classes will respect the same case as the xsd spec
     */
    private final XmlMapper xmlMapper = (XmlMapper) new XmlMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);


    public CreateEventMessage deserializeJsonMessage(String json) throws JsonProcessingException {
        return objectMapper.readValue(json, CreateEventMessage.class);
    }

    public CreateEventMessage deserializeXmlMessage(String xml) throws JsonProcessingException {
        return xmlMapper.readValue(xml, CreateEventMessage.class);
    }

    public String convertToXmlWithTemplate(CreateEventMessage message) throws IOException {
        Template template = getTemplateForFile("template/cisu-create-message");
        return template.apply(message);
    }

    public String convertToXmlWithJackson(CreateEventMessage message) throws JsonProcessingException {
        return xmlMapper.writeValueAsString(message);
    }

    private Template getTemplateForFile(String templateName) throws IOException {
        TemplateLoader loader = new ClassPathTemplateLoader("", ".handlebars");
        Handlebars handlebars = new Handlebars(loader);
        handlebars.setPrettyPrint(true);

        return handlebars.compile(templateName);
    }

    public void validateXML(String message, String xsdFile) throws SAXException, IOException {
        Validator validator = initValidator(xsdFile);
        validator.validate(new StreamSource(new StringReader(message)));
    }

    private Validator initValidator(String xsdPath) throws SAXException {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Source schemaFile = new StreamSource(new File(getClass().getClassLoader().getResource("xsd/" + xsdPath).getFile()));
        Schema schema = factory.newSchema(schemaFile);
        return schema.newValidator();
    }
}
