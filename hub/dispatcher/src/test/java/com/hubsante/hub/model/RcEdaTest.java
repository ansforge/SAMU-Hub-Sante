package com.hubsante.hub.model;

import com.hubsante.model.cisu.CreateCaseMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RcEdaTest extends AbstractModelTest {

    @Test
    @DisplayName("should convert RC-EDA from JSON to XML to JSON")
    public void RC_EDA_e2e_conversionTest() throws IOException {
        File RC_EDA_jsonFile = new File(classLoader.getResource("messages/valid/RC-EDA.json").getFile());
        String json = Files.readString(RC_EDA_jsonFile.toPath());

        CreateCaseMessage initialJSON = (CreateCaseMessage) edxlHandler.deserializeJsonEDXL(json).getContentMessage();
        String xml = converter.serializeXmlMessage(initialJSON);
        CreateCaseMessage xmlConverted = (CreateCaseMessage) converter.deserializeXmlMessage(xml);

        assertEquals(initialJSON, xmlConverted);
        assertDoesNotThrow(() -> validator.validateContentMessage(initialJSON, false));
        // TODO bbo : uncomment next assertion when xsd will be ready
//        assertDoesNotThrow(() -> validator.validateContentMessage(xmlConverted, true));
    }
}
