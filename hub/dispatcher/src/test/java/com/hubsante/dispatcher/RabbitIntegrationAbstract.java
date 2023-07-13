package com.hubsante.dispatcher;

import com.hubsante.hub.HubApplication;
import com.hubsante.hub.service.EdxlHandler;
import com.rabbitmq.client.DefaultSaslConfig;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import javax.net.ssl.SSLContext;
import java.io.IOException;

import static com.hubsante.dispatcher.utils.SSLTestUtils.getSSlContext;

@SpringBootTest
@ContextConfiguration(classes = HubApplication.class, initializers = RabbitIntegrationTest.Initializer.class)
@Testcontainers
@Slf4j
public class RabbitIntegrationAbstract {

    protected static final String HUBSANTE_EXCHANGE = "hubsante";
    protected static final String RABBITMQ_IMAGE = "rabbitmq:3.11-management-alpine";
    protected static final String SAMU_B_OUTER_MESSAGE_ROUTING_KEY = "fr.health.samuB";
    protected static final String SAMU_B_WRONG_OUTER_MESSAGE_ROUTING_KEY = "fr.health.samuB.suffix";
    protected static final String SAMU_A_OUTER_MESSAGE_ROUTING_KEY = "fr.health.samuA";
    protected static final String SAMU_B_ACK_QUEUE = "fr.health.samuB.ack";
    protected static final String SDIS_Z_MESSAGE_QUEUE = "fr.fire.nexsis.sdisZ.message";

    @Autowired
    protected RabbitTemplate rabbitTemplate;

    static ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    @Autowired
    protected EdxlHandler converter;
    protected volatile boolean failed = false;

    @Container
    public static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer(
            DockerImageName.parse(RABBITMQ_IMAGE))
            .withPluginsEnabled("rabbitmq_management", "rabbitmq_auth_mechanism_ssl")
            .withCopyFileToContainer(mountFile("config/definitions.json"),
                    "/tmp/rabbitmq/config/definitions.json")
            .withCopyFileToContainer(mountFile("config/certs/server/"),
                    "/etc/rabbitmq-tls/")
            .withRabbitMQConfigSysctl(mountFile("config/rabbitmq.conf"));

    @BeforeAll
    public static void beforeAll() throws IOException, InterruptedException {
        rabbitMQContainer.start();
        // only for debug : to see the management console
        Integer port = rabbitMQContainer.getMappedPort(15672);
        rabbitMQContainer.execInContainer("rabbitmqctl", "import_definitions", "/tmp/rabbitmq/config/definitions.json");
    }

    @AfterEach
    public void cleanUp() throws IOException, InterruptedException {
        rabbitMQContainer.execInContainer("rabbitmqctl", "purge_queue");
        failed = false;
    }

    protected RabbitTemplate getCustomRabbitTemplate(String p12Path, String p12Passphrase) throws Exception {
        com.rabbitmq.client.ConnectionFactory cf = new com.rabbitmq.client.ConnectionFactory();
        cf.setHost(rabbitMQContainer.getHost());
        cf.setPort(rabbitMQContainer.getAmqpsPort());
        cf.setVirtualHost("/");

        SSLContext sslContext = getSSlContext(p12Path, p12Passphrase);
        cf.useSslProtocol(sslContext);

        cf.setSaslConfig(DefaultSaslConfig.EXTERNAL);
        CachingConnectionFactory ccf = new CachingConnectionFactory(cf);
        ccf.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        ccf.setPublisherReturns(true);

        return new RabbitTemplate(ccf);
    }

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            val values = TestPropertyValues.of(
                    // broker identification
                    "spring.rabbitmq.host=" + rabbitMQContainer.getHost(),
                    "spring.rabbitmq.port=" + rabbitMQContainer.getAmqpsPort(),

                    // default RabbitTemplate conf (dispatcher)
                    "spring.rabbitmq.ssl.key-store-password=dispatcher",
                    "spring.rabbitmq.ssl.trust-store-password=trustStore",
                    "spring.rabbitmq.ssl.key-store=" + Thread.currentThread().getContextClassLoader()
                            .getResource("config/certs/dispatcher/dispatcher.test.p12"),
                    "spring.rabbitmq.ssl.trust-store=" + Thread.currentThread().getContextClassLoader()
                            .getResource("config/certs/trustStore"),
                    "client.preferences.file=" + Thread.currentThread().getContextClassLoader()
                            .getResource("config/client.preferences.csv"),

                    // must be set to handle PublisherConfirms in other RabbitTemplates,
                    // even if we don't use it in Dispatcher
                    "spring.rabbitmq.publisher-confirm-type=correlated",
                    "spring.rabbitmq.publisher-returns=true",
                    "spring.rabbitmq.template.mandatory=true"
            );
            values.applyTo(applicationContext);
        }
    }

    private static MountableFile mountFile(String pathInClasspath) {
        return MountableFile.forClasspathResource(pathInClasspath);
    }
}
