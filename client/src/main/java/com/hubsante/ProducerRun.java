package com.hubsante;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hubsante.model.edxl.EdxlMessage;

import java.nio.file.Files;
import java.nio.file.Path;

import static com.hubsante.Utils.getRouting;
import static com.hubsante.Utils.isJsonScheme;

public class ProducerRun {
    private static final String HUB_HOSTNAME = "hubsante.esante.gouv.fr";
    private static final int HUB_PORT = 5671;
    private static final String EXCHANGE_NAME = "hubsante";

    public static void main(String[] args) throws Exception {
        String routingKey = getRouting(args);
        String fileType = args[1];
        String messageString = Files.readString(Path.of(args[2]));

        TLSConf tlsConf = new TLSConf(
                "TLSv1.2",
                "certPassword",
                "../certs/client.p12",
                "trustStore",
                "../certs/trustStore");

        Producer producer = new Producer(HUB_HOSTNAME, HUB_PORT, EXCHANGE_NAME);
        producer.connect(tlsConf);

        // registering extra module is mandatory to handle date time
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);

        XmlMapper xmlMapper = (XmlMapper) new XmlMapper()
                .registerModule(new JavaTimeModule())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);

        if (isJsonScheme(fileType)) {
            EdxlMessage edxlMessage = mapper.readValue(messageString, EdxlMessage.class);
            producer.publish(routingKey, edxlMessage);
        } else {
            EdxlMessage edxlMessage = xmlMapper.readValue(messageString, EdxlMessage.class);
            producer.xmlPublish(routingKey, edxlMessage);
        }
    }
}
