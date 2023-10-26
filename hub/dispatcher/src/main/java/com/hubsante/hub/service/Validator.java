package com.hubsante.hub.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubsante.hub.exception.SchemaValidationException;
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
import java.util.Set;

@Slf4j
@Service
public class Validator {

    @Autowired
    private ObjectMapper jsonMapper;

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
        // If the schema file references local sub-schema, it must reference the classpath as its id
        // the library behavior is to construct an URI from the id, and then add the schema file name to it
        // if the id is a http URI, it will try to download the subschema online
        // Ref.: https://github.com/networknt/json-schema-validator/issues/55
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
}
