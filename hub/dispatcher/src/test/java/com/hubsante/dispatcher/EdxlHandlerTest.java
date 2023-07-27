package com.hubsante.dispatcher;

import com.hubsante.hub.HubApplication;
import com.hubsante.hub.exception.JsonSchemaValidationException;
import com.hubsante.hub.service.EdxlHandler;
import com.hubsante.model.cisu.CreateEventMessage;
import com.hubsante.model.edxl.EdxlEnvelope;
import com.hubsante.model.edxl.EdxlMessage;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.test.context.SpringRabbitTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;


@Slf4j
@SpringBootTest
@SpringBootConfiguration
@ContextConfiguration(classes = HubApplication.class)
@SpringRabbitTest
public class EdxlHandlerTest {

    static ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    @Autowired
    private EdxlHandler converter;

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry propertiesRegistry) {
        propertiesRegistry.add("client.preferences.file",
                () -> Objects.requireNonNull(classLoader.getResource("config/client.preferences.csv")));
    }

    @Test
    @DisplayName("should deserialize Json EDXL - Cisu Create")
    public void deserializeCreateJsonEDXL() throws IOException {

        File edxlCisuCreateFile = new File(classLoader.getResource("messages/cisuCreateEdxl.json").getFile());
        EdxlMessage edxlMessage = converter.deserializeJsonEDXL(Files.readString(edxlCisuCreateFile.toPath()));

        assertEquals("fr.health.samu050", edxlMessage.getSenderID());
        assertEquals(
                OffsetDateTime.parse("2022-09-27T08:23:34+02:00"),
                edxlMessage.getDateTimeSent()
        );

        CreateEventMessage createEventMessage = edxlMessage
                .getContent().getContentObject().getContentWrapper().getEmbeddedContent().getMessage();

        assertEquals(
                "Détresse vitale|Suspicion d'arrêt cardiaque, mort subite",
                createEventMessage
                        .getCreateEvent()
                        .getPrimaryAlert()
                        .getAlertCode()
                        .getHealthMotive()
                        .getLabel()
        );
    }

    @Test
    @DisplayName("should serialize XML EDXL - Cisu Create")
    public void serializeCreateXmlEDXL() throws IOException, SAXException {
        File edxlCisuCreateFile = new File(classLoader.getResource("messages/cisuCreateEdxl.json").getFile());
        EdxlMessage edxlMessage = converter.deserializeJsonEDXL(Files.readString(edxlCisuCreateFile.toPath()));

        String xml = converter.serializeXmlEDXL(edxlMessage);
        Assertions.assertTrue(() -> xml.startsWith(xmlPrefix()));

        EdxlMessage deserializedFromXml = converter.deserializeXmlEDXL(xml);
        assertEquals(deserializedFromXml, edxlMessage);

        assertDoesNotThrow(() -> converter.validateXML(xml, "edxl/edxl-de-v2.0-wd11.xsd"));
    }

    @Test
    @DisplayName("validation should failed if Json Edxl is malfromatted")
    public void wrongJsonValidationFailed() throws IOException {
        File edxlCisuCreateFile = new File(classLoader.getResource("messages/missingRootAndChildRequiredValues.json").getFile());
        String json = Files.readString(edxlCisuCreateFile.toPath());

        // deserialization method does not throw error
        assertDoesNotThrow(() -> converter.deserializeJsonEDXL(json));
        // validation does
        assertThrows(JsonSchemaValidationException.class, () -> converter.validateJSON(json, "edxl.json"));
    }

    private String xmlPrefix() {
        return "<?xml version='1.0' encoding='UTF-8'?>";
    }
}
