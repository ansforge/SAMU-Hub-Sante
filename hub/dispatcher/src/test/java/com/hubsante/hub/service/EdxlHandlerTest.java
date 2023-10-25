package com.hubsante.hub.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hubsante.hub.HubApplication;
import com.hubsante.hub.exception.SchemaValidationException;
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

import static com.hubsante.hub.config.Constants.EDXL_SCHEMA;
import static com.hubsante.hub.config.Constants.ENVELOPE_SCHEMA;
import static com.hubsante.hub.service.utils.TestFileUtils.getMessageString;
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
    @DisplayName("should consistently deserialize then serialize JSON RC-EDA")
    public void end2end_RC_EDA_JSON() throws IOException {
        String json = getMessageString(true, "RC-EDA", false);
        endToEndDeserializationCheck(json, false);
    }

    @Test
    @DisplayName("should consistently deserialize then serialize XML RC-EDA")
    public void end2end_RC_EDA_XML() throws IOException {
        String xml = getMessageString(true, "RC-EDA", true);
        endToEndDeserializationCheck(xml, true);
    }

    @Test
    @DisplayName("should consistently deserialize then serialize JSON RC-REF")
    public void end2end_RC_REF_JSON() throws IOException {
        String json =getMessageString(true, "RC-REF", false);
        endToEndDeserializationCheck(json, false);
    }

    @Test
    @DisplayName("should consistently deserialize then serialize XML RC-REF")
    public void end2end_RC_REF_XML() throws IOException {
        String xml = getMessageString(true, "RC-REF", true);
        endToEndDeserializationCheck(xml, true);
    }

    @Test
    @DisplayName("should consistently deserialize then serialize JSON RS-INFO")
    public void end2end_RS_INFO_JSON() throws IOException {
        String json =getMessageString(true, "RS-INFO", false);
        endToEndDeserializationCheck(json, false);
    }

    @Test
    @DisplayName("should consistently deserialize then serialize XML RS-INFO")
    public void end2end_RS_INFO_XML() throws IOException {
        String xml = getMessageString(true, "RS-INFO", true);
        endToEndDeserializationCheck(xml, true);
    }

    @Test
    @DisplayName("should add XML prefix")
    public void verifyXmlPrefix() throws IOException {
        File jsonFile = new File(classLoader.getResource("messages/valid/RC-EDA/RC-EDA.json").getFile());
        String json = Files.readString(jsonFile.toPath());

        EdxlMessage messageFromInput = converter.deserializeJsonEDXL(json);
        String xml = converter.serializeXmlEDXL(messageFromInput);
        assertTrue(() -> xml.startsWith(xmlPrefix()));
    }

    private void endToEndDeserializationCheck(String input, boolean isXML) throws JsonProcessingException {
        EdxlMessage messageFromInput;
        EdxlMessage messageFromOutput;

        if (isXML) {
            messageFromInput = converter.deserializeXmlEDXL(input);
            String output = converter.serializeXmlEDXL(messageFromInput);
            messageFromOutput = converter.deserializeXmlEDXL(output);
        } else {
            messageFromInput = converter.deserializeJsonEDXL(input);
            String output = converter.serializeJsonEDXL(messageFromInput);
            messageFromOutput = converter.deserializeJsonEDXL(output);
        }

        assertEquals(messageFromInput, messageFromOutput);
    }

    private String xmlPrefix() {
        return "<?xml version='1.0' encoding='UTF-8'?>";
    }
}
