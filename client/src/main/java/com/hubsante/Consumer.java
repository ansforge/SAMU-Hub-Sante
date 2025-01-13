package com.hubsante;

import com.hubsante.model.EdxlHandler;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public abstract class Consumer {

    protected Channel consumeChannel;

    protected Producer producerAck;

    /**
     * identifiant du client
     */
    protected String clientId;

    /**
     * Nom de la file
     */
    protected String queueName;

    /**
     * serveur distant
     */
    private final String host;

    /**
     * port du serveur distant
     */
    private final int port;

    /**
     * vhost
     */
    private final String vhost;

    private final String exchangeName;

    public String getExchangeName() {
        return exchangeName;
    }

    protected final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
            .setDateFormat(new StdDateFormat().withColonInTimeZone(true));

    protected final XmlMapper xmlMapper = (XmlMapper) new XmlMapper()
            .registerModule(new JavaTimeModule())
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);

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
     * Connexion a la file
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
        if (tlsConf != null) {
            factory.useSslProtocol(tlsConf.getSslContext());
        }
        factory.enableHostnameVerification();

        Connection connection = factory.newConnection();
        if (connection != null) {
            // consumeChannel: where messages are received by the client from Hub Santé
            this.consumeChannel = connection.createChannel();

            // passive declare because the user have no rights to create the queue
            this.consumeChannel.queueDeclarePassive(this.queueName);

            // produceChannel: where ack messages are sent to Hub Santé
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
     * Traitement d'un message recu du Hub
     *
     * @param consumerTag
     * @param delivery
     * @return
     */
    protected abstract void deliverCallback(String consumerTag, Delivery delivery) throws IOException;
}
