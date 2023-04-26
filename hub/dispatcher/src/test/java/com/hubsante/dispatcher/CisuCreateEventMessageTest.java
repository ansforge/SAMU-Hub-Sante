package com.hubsante.dispatcher;

import com.hubsante.hub.HubApplication;
import com.hubsante.hub.service.JsonXmlConverter;
import com.hubsante.message.CreateEventMessage;
import lombok.extern.slf4j.Slf4j;
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

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
@SpringBootConfiguration
@ContextConfiguration(classes = HubApplication.class)
// You should change the active profile to test it locally
@ActiveProfiles({"local","bbo"})
public class CisuCreateEventMessageTest {

    @Autowired
    JsonXmlConverter converter;

    @Test
    public void converterTest() throws IOException {

        // deserialize JSON message
        File cisuJsonFile = new File(Thread.currentThread().getContextClassLoader().getResource("createEventMessage.json").getFile());
        CreateEventMessage deserializedJsonMessage = converter.deserializeJsonMessage(Files.readString(cisuJsonFile.toPath()));

        // serialize XML message with template
        String templatedXML = converter.convertToXmlWithTemplate(deserializedJsonMessage);
        log.info("handlebars message :\n{}", templatedXML);

        // serialize XML message with Jackson lib
        String jacksonSerializedXML = converter.convertToXmlWithJackson(deserializedJsonMessage);
        log.info("jackson serialized message :\n{}", jacksonSerializedXML);

        // Validate templated message
        assertDoesNotThrow(() -> converter.validateXML(templatedXML, "cisu.xsd"));

        // Should throw exception since there are case inconsistencies
        assertThrows(SAXParseException.class, () -> converter.validateXML(jacksonSerializedXML, "cisu.xsd"));

        // But both xml messages can be deserialized in a similar object
        // they equal each other and the generated-from-json one
        CreateEventMessage deserializedFromTemplatedXML = converter.deserializeXmlMessage(templatedXML);
        CreateEventMessage deserializedFromJacksonXML = converter.deserializeXmlMessage(jacksonSerializedXML);

        assertEquals(deserializedJsonMessage, deserializedFromTemplatedXML);
        assertEquals(deserializedJsonMessage, deserializedFromJacksonXML);
    }
}
