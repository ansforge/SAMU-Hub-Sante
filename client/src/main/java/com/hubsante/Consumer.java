package com.hubsante;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hubsante.model.cisu.*;
import com.hubsante.model.edxl.*;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
    private String host;

    /**
     * port du serveur distant
     */
    private int port;

    public String getExchangeName() {
        return exchangeName;
    }

    private String exchangeName;

    protected final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);

    protected final XmlMapper xmlMapper = (XmlMapper) new XmlMapper()
            .registerModule(new JavaTimeModule())
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);

    public Consumer(String host, int port, String exchangeName, String queueName, String clientId) {
        super();

        this.clientId = clientId;
        this.queueName = queueName;
        this.host = host;
        this.port = port;
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
        if (tlsConf != null) {
            factory.useSslProtocol(tlsConf.getSslContext());
        }
        factory.enableHostnameVerification();

        Connection connection = factory.newConnection();
        if (connection != null) {
            // consumeChannel: where messages are received by the client from Hub Santé

            this.consumeChannel = connection.createChannel();
            this.consumeChannel.queueDeclare(this.queueName, true, false, false, null);

            // produceChannel: where ack messages are sent to Hub Santé
            this.producerAck = new Producer(this.host, this.port, this.exchangeName);
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

    protected EdxlMessage generateFunctionalAckMessage(EdxlMessage receivedMessage) {

        GenericAckMessage cisuAckMessage = new GenericAckMessage(receivedMessage.getDistributionID());

        // TODO bbo/rfd : choose what to do with scheme : senderID ? hubsante ?
        ExplicitAddress explicitAddress = new ExplicitAddress();
        explicitAddress.setExplicitAddressScheme(receivedMessage.getSenderID());
        explicitAddress.setExplicitAddressValue(receivedMessage.getSenderID());

        Descriptor descriptor = new Descriptor();
        descriptor.setLanguage(receivedMessage.getDescriptor().getLanguage());
        descriptor.setExplicitAddress(explicitAddress);

        return new EdxlMessage(
                clientId + "_" + UUID.randomUUID(),
                clientId,
                OffsetDateTime.of(LocalDateTime.now(), ZoneOffset.of("+02")),
                OffsetDateTime.of(LocalDateTime.now().plusYears(50), ZoneOffset.of("+02")),
                receivedMessage.getDistributionStatus(),
                DistributionKind.ACK,
                descriptor,
                cisuAckMessage
        );
    }
}
