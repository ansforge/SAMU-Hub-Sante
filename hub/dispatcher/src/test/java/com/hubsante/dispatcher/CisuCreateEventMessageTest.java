package com.hubsante.dispatcher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hubsante.hub.HubApplication;
import com.hubsante.hub.exception.JsonSchemaValidationException;
import com.hubsante.hub.service.JsonXmlConverter;
import com.hubsante.message.CreateEventMessage;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.xml.sax.SAXParseException;

import java.io.*;
import java.nio.file.Files;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
@SpringBootConfiguration
@ContextConfiguration(classes = HubApplication.class)
// You should change the active profile to test it locally
@ActiveProfiles({"local", "bbo"})
public class CisuCreateEventMessageTest {

    @Autowired
    JsonXmlConverter converter;

    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    @Test
    @DisplayName("should deserialize JSON CreateEventMessage")
    public void deserializeJsonMessageTest() throws IOException {
        // deserialize JSON message
        File cisuJsonFile = new File(classLoader.getResource("createEventMessage.json").getFile());
        CreateEventMessage deserializedJsonMessage = converter.deserializeJsonMessage(Files.readString(cisuJsonFile.toPath()));

        assertEquals(
                "2608323d-507d-4cbf-bf74-52007f8124ea",
                deserializedJsonMessage.getMessageId());
        assertEquals(
                OffsetDateTime.parse("2022-09-27T08:23:34+02:00"),
                deserializedJsonMessage.getCreateEvent().getCreatedAt());
        assertEquals(
                "Détresse vitale|Suspicion d'arrêt cardiaque, mort subite",
                deserializedJsonMessage.getCreateEvent().getPrimaryAlert().getAlertCode().getHealthMotive().getLabel());
    }

    @Test
    @DisplayName("should deserialize XML CreateEventMessage")
    public void deserializeXmlMessageTest() throws IOException {
        File cisuXmlFile = new File(classLoader.getResource("createEventMessage.xml").getFile());
        CreateEventMessage deserializedJsonMessage = converter.deserializeXmlMessage(Files.readString(cisuXmlFile.toPath()));

        assertEquals(
                "2608323d-507d-4cbf-bf74-52007f8124ea",
                deserializedJsonMessage.getMessageId());
        assertEquals(
                OffsetDateTime.parse("2022-09-27T08:23:34+02:00"),
                deserializedJsonMessage.getCreateEvent().getCreatedAt());
        assertEquals(
                "Détresse vitale|Suspicion d'arrêt cardiaque, mort subite",
                deserializedJsonMessage.getCreateEvent().getPrimaryAlert().getAlertCode().getHealthMotive().getLabel());
    }

    @Test
    @DisplayName("should serialize CreateEventMessage in JSON")
    public void jsonSerializeMessageTest() throws IOException {
        CreateEventMessage createEventMessage = getCreateEventMessageObject("json");
        assertDoesNotThrow(() -> converter.convertToJson(createEventMessage));
    }

    @Test
    @DisplayName("should serialize CreateEventMessage in XML")
    public void xmlSerializeMessageTest() throws IOException {
        CreateEventMessage createEventMessage = getCreateEventMessageObject("xml");
        assertDoesNotThrow(() -> converter.convertToXmlWithTemplate(createEventMessage));
        // TODO bbo: test it with jackson method
    }

    @Test
    @DisplayName("malformed JSON message should fail validation")
    public void wrongJSONdeserializationFailed() throws IOException {
        // required "sender.uri" field is missing
        File cisuJsonFile = new File(classLoader.getResource("missingRequiredCreateMessage.json").getFile());

        // deserialization method does not throw error
        assertDoesNotThrow(() -> converter.deserializeJsonMessage(Files.readString(cisuJsonFile.toPath())));
        // validation does
        assertThrows(JsonSchemaValidationException.class,
                () -> converter.validateJSON(Files.readString(cisuJsonFile.toPath()), "cisu.json"));
    }

    @Test
    @DisplayName("malformed XML message should fail validation")
    public void wrongXMLdeserializationFailed() throws IOException {
        File wrongXmlFile = new File(classLoader.getResource("missingRequiredElement.xml").getFile());

        // deserialization method does not throw error
        assertDoesNotThrow(() -> converter.deserializeXmlMessage(Files.readString(wrongXmlFile.toPath())));
        // validation does
        assertThrows(SAXParseException.class,
                () -> converter.validateXML(Files.readString(wrongXmlFile.toPath()), "cisu.xsd"));
    }

    @Test
    @DisplayName("end to end conversion test")
    public void endToEnd() throws IOException {

        // deserialize JSON message
        File cisuJsonFile = new File(classLoader.getResource("createEventMessage.json").getFile());
        CreateEventMessage deserializedJsonMessage = converter.deserializeJsonMessage(Files.readString(cisuJsonFile.toPath()));

        // serialize XML message with template
        String templatedXML = converter.convertToXmlWithTemplate(deserializedJsonMessage);
        log.info("handlebars message :\n{}", templatedXML);

        // serialize XML message with Jackson lib
        String jacksonSerializedXML = converter.convertToXmlWithJackson(deserializedJsonMessage);
//        log.info("jackson serialized message :\n{}", jacksonSerializedXML);

        // Validate templated message
        assertDoesNotThrow(() -> converter.validateXML(templatedXML, "cisu.xsd"));

        // Should throw exception since there are case inconsistencies
        // ToDo(bbo) : fix this when working on OpenAPI generation
        assertThrows(SAXParseException.class, () -> converter.validateXML(jacksonSerializedXML, "cisu.xsd"));

        // But both xml messages can be deserialized in a similar object
        // they equal each other and the generated-from-json one
        CreateEventMessage deserializedFromTemplatedXML = converter.deserializeXmlMessage(templatedXML);
        CreateEventMessage deserializedFromJacksonXML = converter.deserializeXmlMessage(jacksonSerializedXML);

        assertEquals(deserializedJsonMessage, deserializedFromTemplatedXML);
        assertEquals(deserializedJsonMessage, deserializedFromJacksonXML);

        String serializedJSON = converter.convertToJson(deserializedFromTemplatedXML);
        assertDoesNotThrow(() -> converter.validateJSON(serializedJSON, "cisu.json"));
    }

    private CreateEventMessage getCreateEventMessageObject(String fileType) throws IOException {
        String fileNamePrefix = "createEventMessage.";
        File sourceFile = new File(classLoader.getResource(fileNamePrefix + fileType).getFile());

        if (fileType.equals("xml")) {
            return converter.deserializeXmlMessage(Files.readString(sourceFile.toPath()));
        } else if (fileType.equals("json")) {
            return converter.deserializeJsonMessage(Files.readString(sourceFile.toPath()));
        } else {
            throw new IOException("unrecognized file type");
        }
    }
}
