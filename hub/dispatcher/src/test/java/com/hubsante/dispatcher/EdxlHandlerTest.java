package com.hubsante.dispatcher;

import com.hubsante.hub.HubApplication;
import com.hubsante.hub.exception.SchemaValidationException;
import com.hubsante.hub.service.EdxlHandler;
import com.hubsante.hub.service.Validator;
import com.hubsante.model.cisu.CreateCaseMessage;
import com.hubsante.model.edxl.EdxlMessage;
import com.hubsante.model.edxl.ContentMessage;
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

    @Autowired
    private Validator validator;

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry propertiesRegistry) {
        propertiesRegistry.add("client.preferences.file",
                () -> Objects.requireNonNull(classLoader.getResource("config/client.preferences.csv")));
    }

    @Test
    @DisplayName("should deserialize Json EDXL - Cisu Create")
    public void deserializeCreateJsonEDXL() throws IOException {

        File edxlCisuCreateFile = new File(classLoader.getResource("messages/valid/edxl_encapsulated/samuA_to_nexsis.json").getFile());
        EdxlMessage edxlMessage = converter.deserializeJsonEDXL(Files.readString(edxlCisuCreateFile.toPath()));

        assertEquals("fr.health.samuA", edxlMessage.getSenderID());
        assertEquals(
                OffsetDateTime.parse("2022-07-25T10:04:34+01:00"),
                edxlMessage.getDateTimeSent()
        );

        CreateCaseMessage createCaseMessage = edxlMessage
                .getContent().getContentObject().getContentWrapper().getEmbeddedContent().getMessage();

        assertEquals(
                "Céphalée, migraines, Traumatisme sérieux, plaie intermédiaire",
                createCaseMessage
                        .getInitialAlert()
                        .getAlertCode()
                        .getHealthMotive()
                        .getLabel()
        );
    }

    @Test
    @DisplayName("should serialize XML EDXL - Cisu Create")
    public void serializeCreateXmlEDXL() throws IOException {
        File edxlCisuCreateFile = new File(classLoader.getResource("messages/valid/edxl_encapsulated/samuA_to_nexsis.json").getFile());
        String json = Files.readString(edxlCisuCreateFile.toPath());
        EdxlMessage edxlMessage = converter.deserializeJsonEDXL(json);

        String xml = converter.serializeXmlEDXL(edxlMessage);
        Assertions.assertTrue(() -> xml.startsWith(xmlPrefix()));

        EdxlMessage deserializedFromXml = converter.deserializeXmlEDXL(xml);
        assertEquals(deserializedFromXml, edxlMessage);

        ContentMessage contentMessage = edxlMessage
                .getContent().getContentObject().getContentWrapper().getEmbeddedContent().getMessage();
        assertDoesNotThrow(() -> validator.validateContentMessage(contentMessage, false));

        //TODO team: generate xsd for new cisu model
//        assertDoesNotThrow(() -> converter.validateXML(xml, "edxl/edxl-de-v2.0-wd11.xsd"));
    }

    @Test
    @DisplayName("validation should failed if Json Edxl is malformatted")
    public void wrongJsonValidationFailed() throws IOException {
        File edxlCisuCreateFile = new File(classLoader.getResource("messages/invalid/missingEDXLRequiredValues.json").getFile());
        String json = Files.readString(edxlCisuCreateFile.toPath());

        // deserialization method does not throw error
        assertDoesNotThrow(() -> converter.deserializeJsonEDXL(json));
        // validation does
        assertThrows(SchemaValidationException.class, () -> validator.validateJSON(json, "edxl.json"));
    }

    @Test
    @DisplayName("validation does not fail if envelope is ok and content is not")
    public void edxlValidationSucceedsWithWrongJsonContent() throws IOException {
        File edxlCisuCreateFile = new File(classLoader.getResource("messages/invalid/invalidCreateMessageValidEdxlEnvelope.json").getFile());
        String json = Files.readString(edxlCisuCreateFile.toPath());

        // deserialization method does not throw error
        assertDoesNotThrow(() -> converter.deserializeJsonEDXL(json));
        // neither validation because envelope is ok
        assertDoesNotThrow(() -> validator.validateJSON(json, "edxl.json"));
    }

    private String xmlPrefix() {
        return "<?xml version='1.0' encoding='UTF-8'?>";
    }
}
