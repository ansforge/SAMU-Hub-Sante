package com.hubsante.hub.model;

import com.hubsante.hub.exception.SchemaValidationException;
import com.hubsante.model.report.ErrorReport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

public class RcInfoTest extends AbstractModelTest {

    @Test
    @DisplayName("should deserialize JSON ErrorReport")
    public void _de_serializeErrorReport() throws IOException {
        File errorReportJsonFile = new File(classLoader.getResource("messages/valid/error_report/errorReport.json").getFile());
        String json = Files.readString(errorReportJsonFile.toPath());
        File errorReportXmlFile = new File(classLoader.getResource("messages/valid/error_report/errorReport.xml").getFile());
        String xml = Files.readString(errorReportXmlFile.toPath());

        ErrorReport errorReportFromJson = (ErrorReport) converter.deserializeJsonMessage(json);
        ErrorReport errorReportFromXML = (ErrorReport) converter.deserializeXmlMessage(xml);

        String convertedXML = converter.serializeXmlMessage(errorReportFromJson);
        ErrorReport errorReportFromConvertedXML = (ErrorReport) converter.deserializeXmlMessage(convertedXML);

        assertEquals(errorReportFromJson, errorReportFromXML);
        assertEquals(errorReportFromJson, errorReportFromConvertedXML);

        assertDoesNotThrow(() -> validator.validateContentMessage(errorReportFromJson, false));
    }

    @Test
    @DisplayName("should validate correct JSON RS-INFO")
    @Override
    public void passingJsonValidation() throws IOException {
        File rsInfoFile = new File(classLoader.getResource("messages/valid/RS-INFO.json").getFile());
        String json = Files.readString(rsInfoFile.toPath());

        assertDoesNotThrow(() -> validator.validateJSON(json, "EDXL-DE-full_schema.json"));
    }

    @Test
    @DisplayName("invalid JSON RS-INFO should fail validation")
    @Override
    public void failingJsonValidation() throws IOException {
        File rsInfoFile = new File(classLoader.getResource("messages/failing/RS-INFO/RS-INFO-missing-required-fields.json").getFile());
        String json = Files.readString(rsInfoFile.toPath());

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
