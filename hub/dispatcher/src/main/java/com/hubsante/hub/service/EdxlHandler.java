package com.hubsante.hub.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hubsante.hub.exception.JsonSchemaValidationException;
import com.hubsante.model.edxl.EdxlInnerMessage;
import com.hubsante.model.edxl.EdxlMessage;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Set;

import static java.lang.Enum.valueOf;

@Service
@Slf4j
public class EdxlHandler {

    private XmlMapper xmlMapper;
    private ObjectMapper jsonMapper;

    public EdxlHandler() {
        this.xmlMapper = (XmlMapper) new XmlMapper()
                .registerModule(new JavaTimeModule())
                .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);

        xmlMapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);

        this.jsonMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
    }

    public EdxlMessage deserializeJsonEDXL(String json) throws JsonProcessingException {
        return jsonMapper.readValue(json, EdxlMessage.class);
    }

    public EdxlMessage deserializeXmlEDXL(String xml) throws JsonProcessingException {
        return xmlMapper.readValue(xml, EdxlMessage.class);
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

    public void validateJSON(String message, String jsonSchemaFile) throws IOException, JsonSchemaValidationException {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        JsonSchema jsonSchema = factory.getSchema(Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("json-schema/" + jsonSchemaFile));
        JsonNode jsonNode = jsonMapper.readTree(message);
        Set<ValidationMessage> validationMessages = jsonSchema.validate(jsonNode);

        if (!validationMessages.isEmpty()) {
            StringBuilder errors = new StringBuilder();
            for (ValidationMessage errorMsg : validationMessages) {
                errors.append(errorMsg.getMessage()).append("\n");
            }
            throw new JsonSchemaValidationException(errors.toString());
        }
    }

    public void validateUseCaseMessage(EdxlMessage edxlMessage, boolean isXML)
            throws IOException, JsonSchemaValidationException, SAXException {
        EdxlInnerMessage useCaseMessage = edxlMessage
                .getContent().getContentObject().getContentWrapper().getEmbeddedContent().getMessage();

        switch (valueOf(UseCaseClass.class, useCaseMessage.getClass().getSimpleName())) {
            case CREATE_EVENT:
                if (isXML) {
                    validateXML(serializeXmlEDXL(edxlMessage), "cisu/cisu.xsd");
                    break;
                }
                validateJSON(serializeJsonEDXL(edxlMessage), "cisu.json");
                break;
            case UNKNOWN:
            default:
                if (isXML) {
                    log.error("Can't validate against XSD : class {} has no specified xsd spec",
                            useCaseMessage.getClass().getSimpleName());
                    break;
                }
                log.error("Can't validate against Json-schema : class {} has no specified schema",
                        useCaseMessage.getClass().getSimpleName());
                break;
        }
    }

    public enum UseCaseClass {
        CREATE_EVENT("CreateEventMessage"),
        UNKNOWN("Unknown");

        UseCaseClass(String clazzName) {
        }
    }
}
