package com.hubsante.hub.model;

import com.hubsante.hub.HubApplication;
import com.hubsante.hub.service.ContentMessageHandler;
import com.hubsante.hub.service.EdxlHandler;
import com.hubsante.hub.service.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.test.context.SpringRabbitTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Objects;

@Slf4j
@SpringBootTest
@SpringBootConfiguration
@ContextConfiguration(classes = HubApplication.class)
@SpringRabbitTest
public abstract class AbstractModelTest {

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
}
