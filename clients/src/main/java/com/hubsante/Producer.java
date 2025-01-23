package com.hubsante;

import com.hubsante.model.EdxlHandler;
import com.hubsante.model.edxl.EdxlMessage;
import com.rabbitmq.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

import static com.hubsante.Constants.*;

public class Producer {

    private static final Logger logger = LoggerFactory.getLogger(Producer.class);
    private Channel producerChannel;
    private Connection connection;

    /**
     * Distant server
     */
    private final String host;

    /**
     * Distant server port
     */
    private final int port;

    /**
     * vhost
     */
    private final String vhost;

    /**
     * Exchange name
     */
    private final String exchangeName;

    public Producer(String host, int port, String vhost, String exchangeName) {
        super();
        this.host = host;
        this.port = port;
        this.vhost = vhost;
        this.exchangeName = exchangeName;
    }

    public void connect(TLSConf tlsConf) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();

        factory.setSaslConfig(DefaultSaslConfig.EXTERNAL);
        factory.setHost(this.host);
        factory.setPort(this.port);
        factory.setVirtualHost(this.vhost);
        // Here, configure the connection recovery policies
        factory.setAutomaticRecoveryEnabled(true);
        RecoveryDelayHandler delayHandler = new RecoveryDelayHandler.ExponentialBackoffDelayHandler();
        factory.setRecoveryDelayHandler(delayHandler);
        // Or, you can set a fixed time interval: factory.setNetworkRecoveryInterval(NETWORK_RECOVERY_INTERVAL);


        if (tlsConf != null) {
            factory.useSslProtocol(tlsConf.getSslContext());
        }

        factory.enableHostnameVerification();

        this.connection = factory.newConnection();

        if (connection != null) {
            this.producerChannel = connection.createChannel();
        }
    }

    public void close() throws IOException, TimeoutException {
        this.producerChannel.close();
        this.connection.close();
    }

    public void publish(String routingKey, EdxlMessage edxlMessage) throws IOException {
        if (this.producerChannel == null) {
            logger.warn("Channel producer unreachable, please ensure that connection has been established" +
                    "(Producer.connect() method has been called)");
            throw new IOException("Unconnected AMQP channel");
        }

        // Setting Content Type becomes mandatory to allow correct deserialization in HubSante
        // Only two content types are allowed : application/json and application/xml
        // If not set, HubSante will not be able to deserialize the message and will reject it
        AMQP.BasicProperties properties = new AMQP.BasicProperties().builder()
                .contentType(JSON_CONTENT_TYPE)
                .deliveryMode(2) // set persistent mode (for cloud resilience - no message is stored out of the transit scope)
                .priority(0) // default priority
                .build();

        EdxlHandler edxlHandler = new EdxlHandler();
        byte[] messageBody = edxlHandler.serializeJsonEDXL(edxlMessage).getBytes(StandardCharsets.UTF_8);

        try {
            this.producerChannel.basicPublish(
                    this.exchangeName,
                    routingKey,
                    properties,
                    messageBody);
        } catch (IOException e) {
            logger.error("Could not publish message with id " + edxlMessage.getDistributionID(), e);
            throw e;
        }
    }

    public void xmlPublish(String routingKey, EdxlMessage edxlMessage) throws IOException {
        if (this.producerChannel == null) {
            logger.warn("Channel producer unreachable, please ensure that connection has been established" +
                    "(Producer.connect() method has been called)");
            throw new IOException("Unconnected AMQP channel");
        }



        // Setting Content Type becomes mandatory to allow correct deserialization in HubSante
        // Only two content types are allowed : application/json and application/xml
        // If not set, HubSante will not be able to deserialize the message and will reject it
        AMQP.BasicProperties properties = new AMQP.BasicProperties().builder()
                .contentType(XML_CONTENT_TYPE)
                .deliveryMode(2) // set persistent mode (for cloud resilience - no message is stored out of the transit scope)
                .priority(0) // default priority
                .build();

        EdxlHandler edxlHandler = new EdxlHandler();
        byte[] messageBody = edxlHandler.serializeXmlEDXL(edxlMessage).getBytes(StandardCharsets.UTF_8);

        try {
            this.producerChannel.basicPublish(
                    this.exchangeName,
                    routingKey,
                    properties,
                    messageBody);
        } catch (IOException e) {
            logger.error("Could not publish message with id " + edxlMessage.getDistributionID(), e);
            throw e;
        }
    }
}
