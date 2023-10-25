package com.hubsante.hub.model;

import com.hubsante.hub.exception.SchemaValidationException;
import com.hubsante.model.cisu.CreateCase;
import com.hubsante.model.cisu.CreateCaseMessage;
import com.hubsante.model.edxl.EdxlMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    @DisplayName("should validate correct JSON RC-EDA")
    @Override
    public void passingJsonValidation() throws IOException {
        File rcEdaFile = new File(classLoader.getResource("messages/valid/RC-EDA.json").getFile());
        String json = Files.readString(rcEdaFile.toPath());

        assertDoesNotThrow(() -> validator.validateJSON(json, "EDXL-DE-full_schema.json"));
    }

    @Test
    @DisplayName("invalid JSON RC-EDA should fail validation ")
    @Override
    public void failingJsonValidation() throws IOException {
        File cisuJsonFile = new File(classLoader.getResource("messages/failing/RC-EDA/RC-EDA-missing-required-fields.json").getFile());
        String json = Files.readString(cisuJsonFile.toPath());

        assertThrows(SchemaValidationException.class, () -> validator.validateJSON(json, "EDXL-DE-full_schema.json"));
    }

    @Test
    @DisplayName("should deserialize JSON RC-EDA")
    @Override
    public void passingJsonDeserialization() throws IOException {
        File rcEdaFile = new File(classLoader.getResource("messages/valid/RC-EDA.json").getFile());
        String json = Files.readString(rcEdaFile.toPath());
        EdxlMessage edxlMessage = edxlHandler.deserializeJsonEDXL(json);
        CreateCase createCase = ((CreateCaseMessage) edxlMessage.getContentMessage()).getCreateCase();

        // check String deserialization
        assertEquals("SAMUA-20230725-AF1234", createCase.getCaseId());
        // check date time deserialization
        assertEquals(OffsetDateTime.parse("2022-07-25T10:03:34+01:00"), createCase.getCreatedAt());
        // check number deserialization
        assertEquals(BigDecimal.valueOf(237), createCase.getCaseLocation().getGeometry().getPoint().getCoord().getHeight());
        // check list deserialization
    }

    @Test
    @DisplayName("should deserialize XML RC-EDA")
    @Override
    public void passingXmlDeserialization() throws IOException {
        File rcEdaXmlFile = new File(classLoader.getResource("messages/valid/RC-EDA.xml").getFile());
        String xml = Files.readString(rcEdaXmlFile.toPath());
        EdxlMessage edxlMessage = edxlHandler.deserializeXmlEDXL(xml);
        CreateCase createCase = ((CreateCaseMessage) edxlMessage.getContentMessage()).getCreateCase();

        // check String deserialization
        assertEquals("SAMU069-20230725-AF1234", createCase.getCaseId());
        // check date time deserialization
        assertEquals(OffsetDateTime.parse("2022-07-25T10:03:34+01:00"), createCase.getCreatedAt());
        // check number deserialization
        assertEquals(BigDecimal.valueOf(237), createCase.getCaseLocation().getGeometry().getPoint().getCoord().getHeight());
        // check list deserialization
//        assertEquals(2, deserializedXmlMessage.getCasualties().size());

        assertDoesNotThrow(() -> edxlHandler.serializeXmlEDXL(edxlMessage));
    }

    @Test
    @DisplayName("should convert RC-EDA both ways ")
    @Override
    public void xmlJsonConversion() throws IOException {
        File RC_EDA_jsonFile = new File(classLoader.getResource("messages/valid/RC-EDA.json").getFile());
        String json = Files.readString(RC_EDA_jsonFile.toPath());

        EdxlMessage initialJSON = edxlHandler.deserializeJsonEDXL(json);
        String xml = edxlHandler.serializeXmlEDXL(initialJSON);
        EdxlMessage xmlConverted = edxlHandler.deserializeXmlEDXL(xml);

        assertEquals(initialJSON, xmlConverted);
        assertDoesNotThrow(() -> validator.validateJSON(json, "EDXL-DE-full_schema.json"));
        // TODO bbo : uncomment next assertion when xsd will be ready
//        assertDoesNotThrow(() -> validator.validateXML((xml, "edxl/edxl-de-v2.0-wd11.xsd"));
    }
}
