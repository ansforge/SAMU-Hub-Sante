package com.hubsante.dispatcher;

import com.hubsante.hub.HubApplication;
import com.hubsante.hub.service.EdxlHandler;
import com.hubsante.model.edxl.EdxlMessage;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.*;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePropertiesBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.r2dbc.ConnectionFactoryBuilder;
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


import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;


@SpringBootTest
@ContextConfiguration(classes = HubApplication.class
        , initializers = RabbitIntegrationTest.Initializer.class
)
@ActiveProfiles("test")
@Testcontainers
@Slf4j
public class RabbitIntegrationTest {
    private static final String HUBSANTE_EXCHANGE = "hubsante";
    private static final String RABBITMQ_IMAGE = "rabbitmq:3.11-management-alpine";
    private static final String SENDER_MESSAGE_ROUTING_KEY = "fr.health.samuB.out.message";
    private static final String RECIPIENT_QUEUE = "fr.fire.nexsis.in.message";

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    static ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    @Autowired
    private EdxlHandler converter;

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry propertiesRegistry) {
        propertiesRegistry.add("client.preferences.file",
                () -> Objects.requireNonNull(classLoader.getResource("config/client.preferences.csv")));
    }

    @BeforeAll
    public static void beforeAll() throws IOException, InterruptedException {
        rabbitMQContainer.start();
        // only for debug : to see the management console
        Integer port = rabbitMQContainer.getMappedPort(15672);
        rabbitMQContainer.execInContainer("rabbitmqctl", "import_definitions", "/tmp/rabbitmq/config/definitions.json");
        System.out.println("tutu");
    }

    @AfterEach
    public void cleanUp() {
//        rabbitAdmin.purgeQueue(ENTRY_QUEUE, false);
//        rabbitAdmin.purgeQueue(SENDER_ACK_QUEUE, false);
//        rabbitAdmin.purgeQueue(RECIPIENT_QUEUE, false);
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

    @Test
    @DisplayName("message dispatched to exchange is received by a consumer listening to the right queue")
    public void dispatchTest() throws IOException, InterruptedException, KeyManagementException, NoSuchAlgorithmException {
        File edxlCisuCreateFile = new File(classLoader.getResource("messages/samuB_to_nexsis.xml").getFile());
        String json = Files.readString(edxlCisuCreateFile.toPath());

        MessageProperties properties = MessagePropertiesBuilder.newInstance().setContentType("application/xml").build();
        Message published = new Message(json.getBytes(StandardCharsets.UTF_8), properties);

        rabbitTemplate.sendAndReceive(HUBSANTE_EXCHANGE, SENDER_MESSAGE_ROUTING_KEY, published);

        Thread.sleep(100);
        Message received = rabbitTemplate.receive(RECIPIENT_QUEUE);

        EdxlMessage publishedEdxl = converter.deserializeXmlEDXL(new String(published.getBody(), StandardCharsets.UTF_8));
        EdxlMessage receivedEdxl = converter.deserializeXmlEDXL(new String(received.getBody(), StandardCharsets.UTF_8));
        Assertions.assertEquals(publishedEdxl, receivedEdxl);
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
