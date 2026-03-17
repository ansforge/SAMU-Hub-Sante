package tnr;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hubsante.model.EdxlHandler;
import com.hubsante.model.edxl.EdxlMessage;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultSaslConfig;
import com.rabbitmq.client.Delivery;
import com.rabbitmq.client.RecoveryDelayHandler;

public abstract class Consumer {

    private static final Logger logger = LoggerFactory.getLogger(Consumer.class);
    /**
     * Channel where messages are received by the client from Hub Santé
     */
    protected Channel consumeChannel;

    /**
     * Channel where ACK messages are sent to Hub Santé
     */
    protected Producer producerAck;

    /**
     * Client identifier
     */
    protected final String clientId;

    /**
     * Queue name
     */
    protected final String queueName;

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
    protected final String vhost;

    /**
     * Exchange name
     */
    private final String exchangeName;

    protected final EdxlHandler edxlHandler = new EdxlHandler();

    public String getExchangeName() {
        return exchangeName;
    }

    public Consumer(String host, int port, String vhost, String exchangeName, String queueName, String clientId) {
        super();

        this.clientId = clientId;
        this.queueName = queueName;
        this.host = host;
        this.port = port;
        this.vhost = vhost;
        this.exchangeName = exchangeName;
    }

    /**
     * Connects to the queue using the configuration provided
     *
     * @param tlsConf
     * @throws IOException
     * @throws TimeoutException
     */
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
        RecoveryDelayHandler delayHandler = new RecoveryDelayHandler.ExponentialBackoffDelayHandler();
        factory.setRecoveryDelayHandler(delayHandler);

        if (tlsConf != null) {
            factory.useSslProtocol(tlsConf.getSslContext());
        }

        factory.enableHostnameVerification();

        final Connection connection = factory.newConnection();

        if (connection != null) {
            this.consumeChannel = connection.createChannel();

            // Passive declaration because the user has no rights to create the queue
            this.consumeChannel.queueDeclarePassive(this.queueName);

            this.producerAck = new Producer(this.host, this.port, this.vhost, this.exchangeName);
            this.producerAck.connect(tlsConf);
            this.consumeChannel.basicConsume(this.queueName, false, (String consumerTag, Delivery message) -> {
                String body = new String(message.getBody(), StandardCharsets.UTF_8);
                try {
                    EdxlMessage edxlMessage = edxlHandler.deserializeJsonEDXL(body);
                    logger.info("Received message from sender: {} with distributionId {}", edxlMessage.getSenderID(), edxlMessage.getDistributionID());
                } catch (JsonProcessingException e) {
                    logger.error("Could not parse sender ID from message: {}", e.getMessage());
                }
                logger.debug("Message body: {}", body);
                deliverCallback(consumerTag, message);
            }, consumerTag -> {
            });

        }

    }
    public String getClientId(){
        return clientId;
    }

    /**
     * Processes the received message from the Hub Santé
     *
     * @param consumerTag
     * @param delivery
     * @return
     */
    protected abstract void deliverCallback(String consumerTag, Delivery delivery) throws IOException;
}
