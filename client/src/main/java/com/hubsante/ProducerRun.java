package com.hubsante;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hubsante.model.edxl.EdxlMessage;

import java.nio.file.Files;
import java.nio.file.Path;

import static com.hubsante.Utils.getRouting;

public class ProducerRun {
    private static final String HUB_HOSTNAME = "localhost";
    private static final int HUB_PORT = 5671;
    private static final String EXCHANGE_NAME = "hubsante";

    public static void main(String[] args) throws Exception {
        String routingKey = getRouting(args);
        String json = Files.readString(Path.of(args[1]));

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
        EdxlMessage edxlMessage = mapper.readValue(json, EdxlMessage.class);

        producer.publish(routingKey, edxlMessage);
    }
}
