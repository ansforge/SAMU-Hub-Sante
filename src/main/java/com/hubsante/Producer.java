package com.hubsante;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

public class Producer {

    private Channel channelProducer;
    /** serveur distant */
    private String host;

    /** port du serveur distant */
    private int port;

    private String exchangeName;

    public Producer(String host, int port, String exchangeName) {
        super();
        this.host = host;
        this.port = port;
        this.exchangeName = exchangeName;
    }

    public void connect(TLSConf tlsConf) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(this.host);
        factory.setPort(this.port);
        if (tlsConf != null) {
            factory.useSslProtocol(tlsConf.getSslContext());
        }
        factory.enableHostnameVerification();
        Connection connection = factory.newConnection();
        if (connection != null) {
            this.channelProducer = connection.createChannel();
            this.channelProducer.exchangeDeclare(this.exchangeName, BuiltinExchangeType.TOPIC, true);
        }
    }

    /**
     * Publication d'un message
     *
     * @param routingKey
     * @param msg
     * @throws IOException
     */
    public void publish(String routingKey, CisuMessage msg) throws IOException {
        this.channelProducer.basicPublish(
                this.exchangeName,
                routingKey,
                MessageProperties.PERSISTENT_TEXT_PLAIN,
                msg.toJsonString().getBytes(StandardCharsets.UTF_8));
    }
}
