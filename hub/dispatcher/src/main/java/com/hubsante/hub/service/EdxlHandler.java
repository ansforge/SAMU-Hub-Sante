package com.hubsante.hub.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.hubsante.hub.exception.JsonSchemaValidationException;
import com.hubsante.model.cisu.CreateCaseMessage;
import com.hubsante.model.edxl.UseCaseMessage;
import com.hubsante.model.edxl.EdxlMessage;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import java.util.Arrays;
import java.util.Set;

@Service
@Slf4j
public class EdxlHandler {

    @Qualifier("rootXmlMapper")
    @Autowired
    private XmlMapper xmlMapper;

    @Autowired
    private ObjectMapper jsonMapper;

    @Autowired
    private UseCaseMessageHandler useCaseMessageHandler;

    public EdxlHandler() {
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
        UseCaseMessage useCaseMessage = edxlMessage
                .getContent().getContentObject().getContentWrapper().getEmbeddedContent().getMessage();

        UseCaseEnum useCase = UseCaseEnum.getByValue(useCaseMessage.getClass().getSimpleName());

        switch (useCase) {
            case CREATE_CASE:
                if (isXML) {
                    validateXML(
                            useCaseMessageHandler.serializeXmlMessage(useCaseMessage),
                            "cisu/cisu.xsd");
                    break;
                }
                validateJSON(
                        useCaseMessageHandler.serializeJsonMessage(useCaseMessage),
                        "createCase_schema.json");
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

    public enum UseCaseEnum {
        CREATE_CASE("CreateCaseMessage"),
        UNKNOWN("Unknown");

        private String clazzName;

        UseCaseEnum(String clazzName) {
            this.clazzName = clazzName;
        }

        public static final UseCaseEnum getByValue(String clazzName) {
            return Arrays.stream(UseCaseEnum.values())
                    .filter(useCaseEnum -> useCaseEnum.clazzName.equals(clazzName)).findFirst().orElse(UNKNOWN);
        }
    }
}
