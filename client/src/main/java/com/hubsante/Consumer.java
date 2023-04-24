package com.hubsante;

import com.hubsante.message.*;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

public abstract class Consumer {

    protected Channel consumeChannel;

    protected Producer producerAck;

    /** identifiant du client */
    protected String clientId;

    /** Nom de la file */
    protected String routingKey;

    /** Nom de la file ack */
    protected String fileAckName;

    /** serveur distant */
    private String host;

    /** port du serveur distant */
    private int port;

    public String getExchangeName() {
        return exchangeName;
    }

    private String exchangeName;

    public Consumer(String host, int port, String exchangeName, String routingKey, String fileAckName, String clientId) {
        super();

        this.clientId = clientId;
        this.routingKey = routingKey;
        this.fileAckName = fileAckName;
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
            this.consumeChannel.queueDeclare(this.routingKey, true, false, false, null);

            // produceChannel: where ack messages are sent to Hub Santé
            this.producerAck = new Producer(this.host, this.port, this.exchangeName);
            this.producerAck.connect(tlsConf);
            this.consumeChannel.basicConsume(this.routingKey, false, new DeliverCallback() {

                @Override
                public void handle(String consumerTag, Delivery message) throws IOException {
                    deliverCallback(consumerTag, message);
                }
            }, consumerTag -> {});

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

    protected AckMessage generateFunctionalAckMessage(BasicMessage receivedMessage) {
        AddresseeType[] recipients = new AddresseeType[]{receivedMessage.getSender()};
        return new AckMessage(
                receivedMessage.getMessageId(),
                new AddresseeType(clientId, "hubsante." + clientId),
                //TODO (bbo) : switch date format in model classes (generator reworking ?)
                // in order to use ZonedDateTime instead:
                // ZoneDateTime.of(LocalDateTime.now(), ZoneId.of(Europe/Paris)) will handle automatically hour change
                OffsetDateTime.of(LocalDateTime.now(), ZoneOffset.of("+02")),
                MsgType.ACK,
                receivedMessage.getStatus(),
                new Recipients(recipients),
                new AckMessageId(UUID.randomUUID().toString())
        );
    }
}
