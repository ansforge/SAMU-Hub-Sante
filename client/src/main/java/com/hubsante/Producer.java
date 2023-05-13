package com.hubsante;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hubsante.model.edxl.EdxlMessage;
import com.rabbitmq.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

public class Producer {

    private static final Logger log = LoggerFactory.getLogger(Producer.class);
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
        factory.setSaslConfig(DefaultSaslConfig.EXTERNAL);

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
    public void publish(String routingKey, EdxlMessage msg) throws IOException {
        if(this.channelProducer == null) {
            log.warn("Channel producer unreachable, please ensure that connection has been established" +
                    "(Producer.connect() method has been called)");
            throw new IOException("Unconnected AMQP channel");
        }

        // registering extra module is mandatory to correctly handle DateTimes
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                // required to preserve offset
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        try {
            this.channelProducer.basicPublish(
                    this.exchangeName,
                    routingKey,
                    MessageProperties.PERSISTENT_TEXT_PLAIN,
                    mapper.writerWithDefaultPrettyPrinter().writeValueAsString(msg).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            // we log error here and propagate the exception to handle it in the business layer
            log.error("Could not publish message with id " + msg.getDistributionID(), e);
            throw e;
        }
    }

    public void xmlPublish(String routingKey, EdxlMessage msg) throws IOException {
        if(this.channelProducer == null) {
            log.warn("Channel producer unreachable, please ensure that connection has been established" +
                    "(Producer.connect() method has been called)");
            throw new IOException("Unconnected AMQP channel");
        }

        XmlMapper xmlMapper = (XmlMapper) new XmlMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);

        xmlMapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);

        try {
            this.channelProducer.basicPublish(
                    this.exchangeName,
                    routingKey,
                    MessageProperties.PERSISTENT_TEXT_PLAIN,
                    xmlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(msg).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            // we log error here and propagate the exception to handle it in the business layer
            log.error("Could not publish message with id " + msg.getDistributionID(), e);
            throw e;
        }
    }
}
