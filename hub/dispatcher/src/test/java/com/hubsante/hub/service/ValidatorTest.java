package com.hubsante.hub.service;

import com.hubsante.hub.HubApplication;
import com.hubsante.hub.exception.SchemaValidationException;
import com.hubsante.model.cisu.CreateCaseMessage;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Objects;

import static com.hubsante.hub.config.Constants.EDXL_SCHEMA;
import static com.hubsante.hub.config.Constants.ENVELOPE_SCHEMA;
import static com.hubsante.hub.service.utils.TestFileUtils.getMessageString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
@SpringBootTest
@SpringBootConfiguration
@ContextConfiguration(classes = HubApplication.class)
@SpringRabbitTest
public class ValidatorTest {
    static ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    @Autowired
    private Validator validator;
    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry propertiesRegistry) {
        propertiesRegistry.add("client.preferences.file",
                () -> Objects.requireNonNull(classLoader.getResource("config/client.preferences.csv")));
    }

    /*
    * For now we chose to be a little verbose and test each message type separately,
    * to best identify eventual errors & postpone xsd validation
    * We could later use an array of UseCases and iterate over it in a single test
    * It will be relevant when first UseCases will be stable and we will be in an incremental development phase
     */
    @Test
    @DisplayName("RC-EDA validation passes")
    public void jsonRcEdaValidationPasses() throws IOException {
        String input = getMessageString(true, "RC-EDA", false);
        assertDoesNotThrow(() -> validator.validateJSON(input, EDXL_SCHEMA));

        // TODO bbo: add XML validation
    }

    @Test
    @DisplayName("RC-EDA validation fails")
    public void jsonRcEdaValidationFails() throws IOException {
        String input = getMessageString(false, "RC-EDA", false);
        assertThrows(SchemaValidationException.class, () -> validator.validateJSON(input, EDXL_SCHEMA));

        // TODO bbo: add XML validation
    }

    @Test
    @DisplayName("RC-REF validation passes")
    public void jsonRcRefValidationPasses() throws IOException {
        String input = getMessageString(true, "RC-REF", false);
        assertDoesNotThrow(() -> validator.validateJSON(input, EDXL_SCHEMA));

        // TODO bbo: add XML validation
    }

    @Test
    @DisplayName("RC-REF validation fails")
    public void jsonRcRefValidationFails() throws IOException {
        String input = getMessageString(false, "RC-REF", false);
        assertThrows(SchemaValidationException.class, () -> validator.validateJSON(input, EDXL_SCHEMA));

        // TODO bbo: add XML validation
    }

    @Test
    @DisplayName("RS-INFO validation passes")
    public void jsonRsInfoValidationPasses() throws IOException {
        String input = getMessageString(true, "RS-INFO", false);
        assertDoesNotThrow(() -> validator.validateJSON(input, EDXL_SCHEMA));

        // TODO bbo: add XML validation
    }

    @Test
    @DisplayName("RS-INFO validation fails")
    public void jsonRsInfoValidationFails() throws IOException {
        String input = getMessageString(false, "RS-INFO", false);
        assertThrows(SchemaValidationException.class, () -> validator.validateJSON(input, EDXL_SCHEMA));

        // TODO bbo: add XML validation
    }

    @Test
    @DisplayName("invalid content valid enveloppe")
    public void invalidContentValidEnvelopeTest() throws IOException {
        File invalidFile = new File(classLoader.getResource("messages/failing/RC-EDA/invalid-RC-EDA-valid-EDXL.json").getFile());
        String json = Files.readString(invalidFile.toPath());

        // envelope validation does not throw because envelope is ok
        assertDoesNotThrow(() -> validator.validateJSON(json, ENVELOPE_SCHEMA));
        assertThrows(SchemaValidationException.class, () -> validator.validateJSON(json, EDXL_SCHEMA));
    }


}