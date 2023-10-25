package com.hubsante.hub.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubsante.hub.exception.SchemaValidationException;
import com.hubsante.model.cisu.CreateCaseMessage;
import com.hubsante.model.edxl.ContentMessage;
import com.hubsante.model.edxl.EdxlMessage;
import com.networknt.schema.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Set;

@Slf4j
@Service
public class Validator {

    @Autowired
    private ObjectMapper jsonMapper;

    @Autowired
    private ContentMessageHandler contentMessageHandler;

    public void validateContentMessage(EdxlMessage edxlMessage, boolean isXML) throws IOException {
        ContentMessage contentMessage = edxlMessage
                .getContent().getContentObject().getContentWrapper().getEmbeddedContent().getMessage();
        validateContentMessage(contentMessage, isXML);
    }
    public void validateContentMessage(ContentMessage contentMessage, boolean isXML)
            throws IOException {
        UseCaseEnum useCase = UseCaseEnum.getByValue(contentMessage.getClass().getSimpleName());

        switch (useCase) {
            case RC_EDA:
                if (isXML) {
                    validateXML(
                        contentMessageHandler.serializeXmlMessage(contentMessage),
                        "cisu/createCase.xsd");
                    break;
                }
                validateJSON(
                        contentMessageHandler.serializeJsonMessage(contentMessage),
                        "RC-DE_schema.json");
                validateJSON(contentMessageHandler.serializeJsonCreateCase((CreateCaseMessage) contentMessage),
                        "RC-EDA_schema.json");
                break;
            //TODO bbo: generate json-schema & xsd for ACK and REPORT
            case CUSTOM:
            case RC_REF:
            case ERROR_REPORT:
            default:
                if (isXML) {
                    log.warn("Can't validate against XSD : class {} has no specified xsd spec",
                            contentMessage.getClass().getSimpleName());
                    break;
                }
                log.warn("Can't validate against Json-schema : class {} has no specified schema",
                        contentMessage.getClass().getSimpleName());
                break;
        }
    }

    public void validateXML(String message, String xsdFile) throws IOException {
        try {
            javax.xml.validation.Validator validator = initValidator(xsdFile);
            validator.validate(new StreamSource(new StringReader(message)));
        } catch (SAXException e) {
            // TODO bbo: check what message is wrapped by SAXException
            throw new SchemaValidationException("Could not validate message against schema : errors occurred. \n" + e.getMessage());
        }

    }

    private javax.xml.validation.Validator initValidator(String xsdPath) throws SAXException {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Source schemaFile = new StreamSource(new File(getClass().getClassLoader().getResource("xsd/" + xsdPath).getFile()));
        Schema schema = factory.newSchema(schemaFile);
        return schema.newValidator();
    }

    public void validateJSON(String message, String jsonSchemaFile) throws IOException {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        InputStream schemaStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("json-schema/" + jsonSchemaFile);
        JsonSchema jsonSchema = factory.getSchema(schemaStream);

        JsonNode jsonNode = jsonMapper.readTree(message);
        Set<ValidationMessage> validationMessages = jsonSchema.validate(jsonNode);

        if (!validationMessages.isEmpty()) {
            StringBuilder errors = new StringBuilder();
            for (ValidationMessage errorMsg : validationMessages) {
                errors.append(errorMsg.getMessage()).append("\n");
            }
            throw new SchemaValidationException("Could not validate message against schema : errors occurred. \n" + errors);
        }
    }

    public enum UseCaseEnum {
        RC_EDA("CreateCaseMessage"),
        RC_REF("ReferenceMessage"),
        CUSTOM("CustomMessage"),
        ERROR_REPORT("ErrorReport");

        private String clazzName;

        UseCaseEnum(String clazzName) {
            this.clazzName = clazzName;
        }

        public static UseCaseEnum getByValue(String clazzName) {
            return Arrays.stream(UseCaseEnum.values())
                    .filter(useCaseEnum -> useCaseEnum.clazzName.equals(clazzName)).findFirst().orElseThrow();
        }
    }
}
