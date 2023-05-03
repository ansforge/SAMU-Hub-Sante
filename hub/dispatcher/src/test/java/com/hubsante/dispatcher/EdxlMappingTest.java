package com.hubsante.dispatcher;

import com.hubsante.hub.HubApplication;
import com.hubsante.hub.service.EdxlMessageConverter;
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
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;


@Slf4j
@SpringBootTest
@SpringBootConfiguration
@ContextConfiguration(classes = HubApplication.class)
@SpringRabbitTest
public class EdxlMappingTest {

    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    @Autowired
    private EdxlMessageConverter converter;

    @Test
    @DisplayName("should deserialize Json EDXL - Envelope Only")
    public void deserializeJsonEnvelope() throws IOException {
        File edxlCisuCreateFile = new File(classLoader.getResource("cisuCreateEdxl.json").getFile());
        String json = Files.readString(edxlCisuCreateFile.toPath());

        EdxlEnvelope envelope = converter.deserializeJsonEnvelope(json);
        System.out.println("u");
        assertEquals("SAMU_001", envelope.getSenderID());
    }

    @Test
    @DisplayName("should deserialize Json EDXL - Cisu Create")
    public void deserializeCreateJsonEDXL() throws IOException {

        File edxlCisuCreateFile = new File(classLoader.getResource("cisuCreateEdxl.json").getFile());
        EdxlMessage edxlMessage = converter.deserializeJsonEDXL(Files.readString(edxlCisuCreateFile.toPath()));

        assertEquals("SAMU_001", edxlMessage.getSenderID());
        assertEquals(
                OffsetDateTime.parse("2022-09-27T08:23:34+02:00"),
                edxlMessage.getDateTimeSent()
        );

        assertEquals(
                "Détresse vitale|Suspicion d'arrêt cardiaque, mort subite",
                edxlMessage
                        .getContent().getContentObject().getContentWrapper().getEmbeddedContent()
                        .getMessage()
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
        File edxlCisuCreateFile = new File(classLoader.getResource("cisuCreateEdxl.json").getFile());
        EdxlMessage edxlMessage = converter.deserializeJsonEDXL(Files.readString(edxlCisuCreateFile.toPath()));

        String xml = converter.serializeXmlEDXL(edxlMessage);
        Assertions.assertTrue(() -> xml.startsWith(xmlPrefix()));

        EdxlMessage deserializedFromXml = converter.deserializeXmlEDXL(xml);
        assertEquals(deserializedFromXml, edxlMessage);

        converter.validateXML(xml, "edxl/edxl-de-v2.0-wd11.xsd");
    }

    private String xmlPrefix() {
        return "<?xml version='1.0' encoding='UTF-8'?>";
    }
}
