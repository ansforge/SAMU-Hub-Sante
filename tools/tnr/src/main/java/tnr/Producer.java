package tnr;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static tnr.Constants.JSON_CONTENT_TYPE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultSaslConfig;
import com.rabbitmq.client.RecoveryDelayHandler;

public class Producer {

    private static final Logger logger = LoggerFactory.getLogger(Producer.class);
    private Channel producerChannel;
    private Connection connection;
    private final String host;
    private final int port;
    private final String vhost;
    private final String exchangeName;

    public Producer(String host, int port, String vhost, String exchangeName) {
        super();
        this.host = host;
        this.port = port;
        this.vhost = vhost;
        this.exchangeName = exchangeName;
    }

    public void connect(TLSConf tlsConf) throws IOException, TimeoutException {
        final ConnectionFactory factory = new ConnectionFactory();

        factory.setSaslConfig(DefaultSaslConfig.EXTERNAL);
        factory.setHost(this.host);
        factory.setPort(this.port);
        factory.setVirtualHost(this.vhost);

        // Here, configure the connection recovery policies
        // NB - You can set a fixed time interval using setNetworkRecoveryInterval(NETWORK_RECOVERY_INTERVAL);
        // NB - You can optionally configure ExponentialBackoffDelayHandler with your own backoff sequence.
        factory.setAutomaticRecoveryEnabled(true);
        final RecoveryDelayHandler delayHandler = new RecoveryDelayHandler.ExponentialBackoffDelayHandler();
        factory.setRecoveryDelayHandler(delayHandler);

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

    public void publish(String routingKey, byte[] messageBody) throws IOException {
        if (this.producerChannel == null) {
            logger.warn("Channel producer unreachable, please ensure that connection has been established"
                    + "(Producer.connect() method has been called)");
            throw new IOException("Unconnected AMQP channel");
        }

        // Setting Content Type becomes mandatory to allow correct deserialization in HubSante
        // Only two content types are allowed : application/json and application/xml
        // If not set, HubSante will not be able to deserialize the message and will reject it
        final AMQP.BasicProperties properties = new AMQP.BasicProperties().builder()
                .contentType(JSON_CONTENT_TYPE)
                .deliveryMode(2) // set persistent mode (for cloud resilience - no message is stored out of the transit scope)
                .priority(0) // default priority
                .build();
        try {
            this.producerChannel.basicPublish(
                    this.exchangeName,
                    routingKey,
                    properties,
                    messageBody);
        } catch (IOException e) {
            logger.error("Could not publish message.");
            throw e;
        }
    }
}
