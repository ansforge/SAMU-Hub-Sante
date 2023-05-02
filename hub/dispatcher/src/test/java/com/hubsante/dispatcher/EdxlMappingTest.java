package com.hubsante.dispatcher;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hubsante.hub.HubApplication;
import com.hubsante.model.edxl.EdxlMessage;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;


@Slf4j
@SpringBootTest
@SpringBootConfiguration
@ContextConfiguration(classes = HubApplication.class)
// You should change the active profile to test it locally
@ActiveProfiles({"local", "bbo"})
public class EdxlMappingTest {

    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    ObjectMapper jsonMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);

    XmlMapper xmlMapper = (XmlMapper) new XmlMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);


    @Test
    @DisplayName("should deserialize Json EDXL - Cisu Create")
    public void deserializeCreateJsonEDXL() throws IOException {

        File edxlCisuCreateFile = new File(classLoader.getResource("cisuCreateEdxl.json").getFile());
        EdxlMessage edxlMessage = jsonMapper.readValue(edxlCisuCreateFile, EdxlMessage.class);

        assertEquals("Origin", edxlMessage.getSenderID());
        assertEquals(
                OffsetDateTime.parse("2022-09-27T08:23:34+02:00"),
                edxlMessage.getDateTimeSent()
        );
        assertEquals(
                "Détresse vitale|Suspicion d'arrêt cardiaque, mort subite",
                edxlMessage.getContent().getContentObject().getContentXML().getEmbeddedXMLContent()
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
    public void serializeCreateXmlEDXL() throws IOException {
        File edxlCisuCreateFile = new File(classLoader.getResource("cisuCreateEdxl.json").getFile());
        EdxlMessage edxlMessage = jsonMapper.readValue(edxlCisuCreateFile, EdxlMessage.class);

        xmlMapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
        String xml = xmlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(edxlMessage);

        EdxlMessage deserializedFromXml = xmlMapper.readValue(xml, EdxlMessage.class);
        assertEquals(deserializedFromXml, edxlMessage);
    }
}
