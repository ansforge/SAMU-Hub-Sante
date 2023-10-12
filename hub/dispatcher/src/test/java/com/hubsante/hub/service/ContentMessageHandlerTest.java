package com.hubsante.hub.service;

import com.hubsante.hub.HubApplication;
import com.hubsante.hub.exception.SchemaValidationException;
import com.hubsante.model.cisu.CreateCase;
import com.hubsante.model.cisu.CreateCaseMessage;
import com.hubsante.model.report.ErrorReport;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.test.context.SpringRabbitTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
@SpringBootConfiguration
@ContextConfiguration(classes = HubApplication.class)
@SpringRabbitTest
public class ContentMessageHandlerTest {

    @Autowired
    private ContentMessageHandler converter;

    @Autowired
    private Validator validator;

    static ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry propertiesRegistry) {
        propertiesRegistry.add("client.preferences.file",
                () -> Objects.requireNonNull(classLoader.getResource("config/client.preferences.csv")));
    }

    @Test
    @DisplayName("should deserialize JSON CreateCaseMessage")
    public void deserializeJSONCreateCaseMessage() throws IOException {
        // deserialize JSON message
        File cisuJsonFile = new File(classLoader.getResource("messages/valid/create_case/createCaseMessage.json").getFile());
        CreateCaseMessage deserializedJsonMessage = (CreateCaseMessage) converter.deserializeJsonMessage(Files.readString(cisuJsonFile.toPath()));
        CreateCase createCase = deserializedJsonMessage.getCreateCase();

        String xml = converter.serializeXmlMessage(deserializedJsonMessage);
        System.out.println(xml);
        // check String deserialization
        assertEquals("SAMUA-20230725-AF1234", createCase.getCaseId());
        // check date time deserialization
        assertEquals(OffsetDateTime.parse("2022-07-25T10:03:34+01:00"), createCase.getCreatedAt());
        // check number deserialization
        assertEquals(BigDecimal.valueOf(237), createCase.getCaseLocation().getGeometry().getPoint().getCoord().getHeight());
        // check list deserialization
//        assertEquals(2, createCase.getCasualties().size());
    }

    @Test
    @DisplayName("should deserialize XML CreateCaseMessage")
    public void deserializeXMLCreateCaseMessage() throws IOException {
        // deserialize XML message
        File cisuXmlFile = new File(classLoader.getResource("messages/valid/create_case/createCaseMessage.xml").getFile());
        CreateCaseMessage deserializedXmlMessage = (CreateCaseMessage) converter.deserializeXmlMessage(Files.readString(cisuXmlFile.toPath()));
        CreateCase createCase = deserializedXmlMessage.getCreateCase();

        // check String deserialization
        assertEquals("SAMU069-20230725-AF1234", createCase.getCaseId());
        // check date time deserialization
        assertEquals(OffsetDateTime.parse("2022-07-25T10:03:34+01:00"), createCase.getCreatedAt());
        // check number deserialization
        assertEquals(BigDecimal.valueOf(237), createCase.getCaseLocation().getGeometry().getPoint().getCoord().getHeight());
        // check list deserialization
//        assertEquals(2, deserializedXmlMessage.getCasualties().size());
    }

    @Test
    @DisplayName("should serialize JSON CreateCaseMessage")
    public void serializeJSONCreateCaseMessage() throws IOException {
        // deserialize JSON message
        File cisuJsonFile = new File(classLoader.getResource("messages/valid/create_case/createCaseMessage.json").getFile());
        CreateCaseMessage deserializedJsonMessage = (CreateCaseMessage) converter.deserializeJsonMessage(Files.readString(cisuJsonFile.toPath()));

        // serialize JSON message
        assertDoesNotThrow(() -> converter.serializeJsonMessage(deserializedJsonMessage));
    }

    @Test
    @DisplayName("should serialize XML CreateCaseMessage")
    public void serializeXMLCreateCaseMessage() throws IOException {
        // deserialize XML message
        File cisuXmlFile = new File(classLoader.getResource("messages/valid/create_case/createCaseMessage.xml").getFile());
        CreateCaseMessage deserializedXmlMessage = (CreateCaseMessage) converter.deserializeXmlMessage(Files.readString(cisuXmlFile.toPath()));

        // serialize XML message
        assertDoesNotThrow(() -> converter.serializeXmlMessage(deserializedXmlMessage));
    }

    @Test
    @DisplayName("missing required fields fails validation")
    public void missingRequiredFieldsValidationFails() throws IOException {
        File cisuJsonFile = new File(classLoader.getResource("messages/invalid/create_case/missingRequiredFieldCreateMessage.json").getFile());

        assertDoesNotThrow(() -> converter.deserializeJsonMessage(Files.readString(cisuJsonFile.toPath())));

        // TODO bbo : check python script, required fields ar not set in json-schema
        assertThrows(SchemaValidationException.class, () -> validator.validateContentMessage(
                converter.deserializeJsonMessage(Files.readString(cisuJsonFile.toPath())), false));
        // TODO bbo : uncomment next assertion when xsd will be ready
//        assertThrows(SAXException.class, () -> validator.validateContentMessage(
//                converter.deserializeXmlMessage(Files.readString(cisuJsonFile.toPath())), true));
    }

    @Test
    @DisplayName("should convert message from JSON to XML to JSON")
    public void endToEndConversionTest() throws IOException {
        File cisuJsonFile = new File(classLoader.getResource("messages/valid/create_case/createCaseMessage.json").getFile());

        String json = Files.readString(cisuJsonFile.toPath());
        CreateCaseMessage initialJSON = (CreateCaseMessage) converter.deserializeJsonMessage(json);

        String xml = converter.serializeXmlMessage(initialJSON);
        CreateCaseMessage xmlConverted = (CreateCaseMessage) converter.deserializeXmlMessage(xml);

        assertEquals(initialJSON, xmlConverted);
        assertDoesNotThrow(() -> validator.validateContentMessage(initialJSON, false));
        // TODO bbo : uncomment next assertion when xsd will be ready
        assertDoesNotThrow(() -> validator.validateContentMessage(xmlConverted, true));
    }

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

        assertEquals(errorReportFromJson.getErrorCode(), errorReportFromXML.getErrorCode());
        assertEquals(errorReportFromXML.getErrorCode(), errorReportFromConvertedXML.getErrorCode());

        assertDoesNotThrow(() -> validator.validateContentMessage(errorReportFromJson, false));
    }
}
