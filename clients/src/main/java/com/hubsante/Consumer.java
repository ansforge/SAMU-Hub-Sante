package com.hubsante;

import com.hubsante.model.EdxlHandler;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static com.hubsante.Constants.NETWORK_RECOVERY_INTERVAL;

public abstract class Consumer {
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
    protected String clientId;

    /**
     * Queue name
     */
    protected String queueName;

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
        ConnectionFactory factory = new ConnectionFactory();

        factory.setSaslConfig(DefaultSaslConfig.EXTERNAL);
        factory.setHost(this.host);
        factory.setPort(this.port);
        factory.setVirtualHost(this.vhost);
        factory.setNetworkRecoveryInterval(NETWORK_RECOVERY_INTERVAL);
        factory.setAutomaticRecoveryEnabled(true);

        if (tlsConf != null) {
            factory.useSslProtocol(tlsConf.getSslContext());
        }

        factory.enableHostnameVerification();

        Connection connection = factory.newConnection();

        if (connection != null) {
            this.consumeChannel = connection.createChannel();

            // Passive declaration because the user has no rights to create the queue
            this.consumeChannel.queueDeclarePassive(this.queueName);

            this.producerAck = new Producer(this.host, this.port, this.vhost, this.exchangeName);
            this.producerAck.connect(tlsConf);
            this.consumeChannel.basicConsume(this.queueName, false, new DeliverCallback() {

                @Override
                public void handle(String consumerTag, Delivery message) throws IOException {
                    deliverCallback(consumerTag, message);
                }
            }, consumerTag -> {
            });

        }
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
