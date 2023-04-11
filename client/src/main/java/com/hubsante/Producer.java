package com.hubsante;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hubsante.message.*;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
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
        // registering extra module is mandatory to correctly handle DateTimes
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

        this.channelProducer.basicPublish(
                this.exchangeName,
                routingKey,
                MessageProperties.PERSISTENT_TEXT_PLAIN,
                mapper.writeValueAsString(msg).getBytes(StandardCharsets.UTF_8));
    }
}
