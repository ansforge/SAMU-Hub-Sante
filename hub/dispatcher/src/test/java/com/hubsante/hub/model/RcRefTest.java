package com.hubsante.hub.model;

import com.hubsante.hub.exception.SchemaValidationException;
import com.hubsante.model.cisu.ReferenceMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

public class RcRefTest extends AbstractModelTest {

    @Test
    @DisplayName("should convert and validate RC-REF from JSON to XML to JSON")
    public void RC_REF_e2e_conversionTest() throws IOException {
        File RC_REF_jsonFile = new File(classLoader.getResource("messages/valid/RC-REF.json").getFile());
        String json = Files.readString(RC_REF_jsonFile.toPath());

        ReferenceMessage initialJSON = (ReferenceMessage) edxlHandler.deserializeJsonEDXL(json).getContentMessage();
        String xml = converter.serializeXmlMessage(initialJSON);
        ReferenceMessage xmlConverted = (ReferenceMessage) converter.deserializeXmlMessage(xml);

        assertEquals(initialJSON, xmlConverted);
        assertDoesNotThrow(() -> validator.validateContentMessage(initialJSON, false));
        // TODO bbo: uncomment next assertion when xsd will be ready
//        assertDoesNotThrow(() -> validator.validateContentMessage(xmlConverted, true));
    }

    @Test
    @DisplayName("should validate correct JSON RC-REF")
    @Override
    public void passingJsonValidation() throws IOException {
        File rcRefFile = new File(classLoader.getResource("messages/valid/RC-REF.json").getFile());
        String json = Files.readString(rcRefFile.toPath());
        assertDoesNotThrow(() -> validator.validateJSON(json, "EDXL-DE-full_schema.json"));
    }

    @Test
    @DisplayName("invalid JSON RC-REF should fail validation ")
    @Override
    public void failingJsonValidation() throws IOException {
        File rcRefFile = new File(classLoader.getResource("messages/failing/RC-REF/RC-REF-missing-required-fields.json").getFile());
        String json = Files.readString(rcRefFile.toPath());
        assertThrows(SchemaValidationException.class, () -> validator.validateJSON(json, "EDXL-DE-full_schema.json"));
    }

    @Override
    public void passingJsonDeserialization() throws IOException {

    }

    @Override
    public void passingXmlDeserialization() {

    }

    @Override
    public void xmlJsonConversion() throws IOException {

    }
}
