/**
 * Copyright © 2023-2026 Agence du Numerique en Sante (ANS)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hubsante.hub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.hubsante.hub.HubApplication;
import com.hubsante.hub.testsupport.HubTestTags;
import com.hubsante.hub.testsupport.SSLTestUtils;
import com.hubsante.model.EdxlHandler;
import com.rabbitmq.client.DefaultSaslConfig;
import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import javax.net.ssl.SSLContext;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

@SpringBootTest
@ContextConfiguration(
        classes = HubApplication.class,
        initializers = RabbitIntegrationAbstract.Initializer.class)
@Testcontainers
@ActiveProfiles("test")
@Tag(HubTestTags.INTEGRATION)
@Slf4j
public class RabbitIntegrationAbstract {

    protected static final String HUBSANTE_EXCHANGE = "hubsante";
    protected static final String RABBITMQ_IMAGE = "rabbitmq:3.12.13-management-alpine";
    protected static final String SAMU_A_ROUTING_KEY = "fr.health.samuA";
    protected static final String SAMU_A_MESSAGE_QUEUE = SAMU_A_ROUTING_KEY + ".message";
    protected static final String SAMU_A_INFO_QUEUE = SAMU_A_ROUTING_KEY + ".info";
    protected static final String SAMU_B_ROUTING_KEY = "fr.health.samuB";
    protected static final String SAMU_B_MESSAGE_QUEUE = SAMU_B_ROUTING_KEY + ".message";
    protected static final String SAMU_B_INFO_QUEUE = SAMU_B_ROUTING_KEY + ".info";

    protected static final String JSON = MessageProperties.CONTENT_TYPE_JSON;

    @Autowired protected RabbitTemplate rabbitTemplate;

    static ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    @Autowired protected EdxlHandler converter;
    protected volatile boolean failed = false;

    @Container
    public static RabbitMQContainer rabbitMQContainer =
            new RabbitMQContainer(DockerImageName.parse(RABBITMQ_IMAGE))
                    .withPluginsEnabled("rabbitmq_management", "rabbitmq_auth_mechanism_ssl")
                    .withCopyFileToContainer(
                            mountFile("config/definitions.json"), "/config/definitions.json")
                    .withCopyFileToContainer(
                            mountFile("config/certs/rabbitmq/"), "/etc/rabbitmq-tls/")
                    .withCopyFileToContainer(
                            mountFile("config/batch-test.sh"), "/tmp/rabbitmq/config/batch-test.sh")
                    .withRabbitMQConfigSysctl(mountFile("config/rabbitmq.conf"));

    @BeforeAll
    public static void beforeAll() throws IOException, InterruptedException {
        rabbitMQContainer.start();
        // only for debug : to see the management console
        Integer port = rabbitMQContainer.getMappedPort(15672);
        rabbitMQContainer.execInContainer("chmod", "+x", "/tmp/rabbitmq/config/batch-test.sh");
    }

    @AfterEach
    public void cleanUp() throws IOException, InterruptedException {
        String[] queues = {
            SAMU_A_INFO_QUEUE, SAMU_A_MESSAGE_QUEUE, SAMU_B_INFO_QUEUE, SAMU_B_MESSAGE_QUEUE
        };
        for (String queue : queues) {
            rabbitMQContainer.execInContainer("rabbitmqctl", "purge_queue", queue);
        }
        failed = false;
    }

    protected RabbitTemplate getCustomRabbitTemplate(String p12Path, String p12Passphrase)
            throws Exception {
        com.rabbitmq.client.ConnectionFactory cf = new com.rabbitmq.client.ConnectionFactory();
        cf.setHost(rabbitMQContainer.getHost());
        cf.setPort(rabbitMQContainer.getAmqpsPort());
        cf.setVirtualHost("15-15_v2.1");

        SSLContext sslContext = SSLTestUtils.getSSlContext(p12Path, p12Passphrase);
        cf.useSslProtocol(sslContext);

        cf.setSaslConfig(DefaultSaslConfig.EXTERNAL);
        CachingConnectionFactory ccf = new CachingConnectionFactory(cf);
        ccf.setPublisherReturns(true);

        return new RabbitTemplate(ccf);
    }

    /** How long a message may take to travel publisher -> hub -> recipient queue. */
    protected static final Duration DELIVERY_TIMEOUT = Duration.ofSeconds(10);

    /** How long "nothing arrived" must hold before we believe it. */
    protected static final Duration QUIET_WINDOW = Duration.ofSeconds(1);

    private static final Duration POLL_INTERVAL = Duration.ofMillis(100);

    /** Waits for a message on {@code queue}, instead of sleeping and hoping. */
    protected Message awaitMessageOn(RabbitTemplate consumer, String queue) {
        return await("message on " + queue)
                .atMost(DELIVERY_TIMEOUT)
                .pollInterval(POLL_INTERVAL)
                .until(() -> consumer.receive(queue), Objects::nonNull);
    }

    /**
     * Lets a quiet window elapse, then checks the queue exactly once.
     *
     * <p>Deliberately not a polling condition: {@code receive} consumes, so polling for "no message"
     * would swallow the message on the first poll and then report success on the second.
     */
    protected void assertNoMessageOn(RabbitTemplate consumer, String queue) {
        await("quiet window on " + queue).pollDelay(QUIET_WINDOW).until(() -> true);
        assertThat(consumer.receive(queue)).as("unexpected message on %s", queue).isNull();
    }

    protected RabbitTemplate clientTemplate(String client) throws Exception {
        return getCustomRabbitTemplate(
                classLoader.getResource("config/certs/" + client + "/" + client + ".p12").getPath(),
                client);
    }

    public static class Initializer
            implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            // only the container-dependent values belong here, the rest is in
            // application.properties / application-test.properties
            val values =
                    TestPropertyValues.of(
                            "spring.rabbitmq.host=" + rabbitMQContainer.getHost(),
                            "spring.rabbitmq.port=" + rabbitMQContainer.getAmqpsPort(),
                            "spring.rabbitmq.virtual-host=15-15_v2.1");
            values.applyTo(applicationContext);
        }
    }

    private static MountableFile mountFile(String pathInClasspath) {
        return MountableFile.forClasspathResource(pathInClasspath);
    }
}
