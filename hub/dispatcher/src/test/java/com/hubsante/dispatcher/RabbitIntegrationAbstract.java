package com.hubsante.dispatcher;

import com.hubsante.hub.HubApplication;
import com.hubsante.hub.service.EdxlHandler;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.util.Objects;

@SpringBootTest
@ContextConfiguration(classes = HubApplication.class
        , initializers = RabbitIntegrationTest.Initializer.class
)
@ActiveProfiles("test")
@Testcontainers
@Slf4j
public class RabbitIntegrationAbstract {

    protected static final String HUBSANTE_EXCHANGE = "hubsante";
    protected static final String RABBITMQ_IMAGE = "rabbitmq:3.11-management-alpine";
    protected static final String ENTRY_QUEUE = "*.out.*";
    protected static final String SENDER_MESSAGE_ROUTING_KEY = "fr.health.samuB.out.message";
    protected static final String SENDER_ACK_QUEUE = "fr.health.samuB.in.ack";
    protected static final String RECIPIENT_QUEUE = "fr.fire.nexsis.in.message";

    @Autowired
    protected RabbitTemplate rabbitTemplate;

    @Autowired
    protected RabbitTemplate samuARabbitTemplate;

    @Autowired
    protected RabbitAdmin rabbitAdmin;

    static ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    @Autowired
    protected EdxlHandler converter;

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry propertiesRegistry) {
        propertiesRegistry.add("client.preferences.file",
                () -> Objects.requireNonNull(classLoader.getResource("config/client.preferences.csv")));
    }

    @Container
    public static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer(
            DockerImageName.parse(RABBITMQ_IMAGE))
            .withPluginsEnabled("rabbitmq_management", "rabbitmq_auth_mechanism_ssl")
            .withCopyFileToContainer(MountableFile.forClasspathResource("config/definitions.json"),
                    "tmp/rabbitmq/config/definitions.json")
            .withCopyFileToContainer(MountableFile.forClasspathResource("config/certs/server/"),
                    "/etc/rabbitmq-tls/")
            .withRabbitMQConfigSysctl(MountableFile.forClasspathResource("config/rabbitmq.conf"));

    @BeforeAll
    public static void beforeAll() throws IOException, InterruptedException {
        rabbitMQContainer.start();
        // only for debug : to see the management console
        Integer port = rabbitMQContainer.getMappedPort(15672);
        rabbitMQContainer.execInContainer("rabbitmqctl", "import_definitions", "/tmp/rabbitmq/config/definitions.json");
    }

    @AfterEach
    public void cleanUp() {
        rabbitAdmin.purgeQueue(ENTRY_QUEUE, false);
        rabbitAdmin.purgeQueue(SENDER_ACK_QUEUE, false);
        rabbitAdmin.purgeQueue(RECIPIENT_QUEUE, false);
    }

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            val values = TestPropertyValues.of(
                    "spring.rabbitmq.host=" + rabbitMQContainer.getHost(),
                    "spring.rabbitmq.port=" + rabbitMQContainer.getMappedPort(5672)
            );
            values.applyTo(applicationContext);
        }
    }
}
