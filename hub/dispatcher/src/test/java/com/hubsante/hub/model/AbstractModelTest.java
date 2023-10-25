package com.hubsante.hub.model;

import com.hubsante.hub.HubApplication;
import com.hubsante.hub.service.ContentMessageHandler;
import com.hubsante.hub.service.EdxlHandler;
import com.hubsante.hub.service.Validator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.test.context.SpringRabbitTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.util.Objects;

@Slf4j
@SpringBootTest
@SpringBootConfiguration
@ContextConfiguration(classes = HubApplication.class)
@SpringRabbitTest
public abstract class AbstractModelTest {

    /*
    * serialization does not throw
    * deserialize JSON: check string, number, date, list
    * deserialize XML: check string, number, date, list
     */

    /* All tests should follow this :
    * -> passing case JSON : validation, deserialization, serialization, validation
    * -> passing case XML : validation, deserialization, serialization, validation
    * -> passing conversion : deserialization from JSON, serialization to XML, deserialization from XML, comparison
    *
    * xml & json should contain string, number, empty lists, lists with one element, lists with multiple elements, null values
    *
    * -> failing validation : missing required fields, invalid fields
    * -> failing deserialization : invalid fields, invalid types
    *
    * e2e : json -> xml -> deserialize, comparison
    */

    @Autowired
    protected ContentMessageHandler converter;

    @Autowired
    protected EdxlHandler edxlHandler;

    @Autowired
    protected Validator validator;

    protected static ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry propertiesRegistry) {
        propertiesRegistry.add("client.preferences.file",
                () -> Objects.requireNonNull(classLoader.getResource("config/client.preferences.csv")));
    }

    @Test
    public abstract void passingJsonValidation() throws IOException;
//    public abstract void passingXmlValidation();
    @Test
    public abstract void failingJsonValidation() throws IOException;
//    public abstract void failingXmlValidation();
    @Test
    public abstract void passingJsonDeserialization() throws IOException;
    @Test
    public abstract void passingXmlDeserialization() throws IOException;
    @Test
    public abstract void xmlJsonConversion() throws IOException;
}
