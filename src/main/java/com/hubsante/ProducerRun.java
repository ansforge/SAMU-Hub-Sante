package com.hubsante;

import com.fasterxml.jackson.databind.ObjectMapper;
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
        ObjectMapper mapper = new ObjectMapper();
        CisuMessage cisuMessage = mapper.readValue(json, CisuMessage.class);

        TLSConf tlsConf = new TLSConf(
                "TLSv1.2",
                "certPassword",
                "certs/client.p12",
                "trustStore",
                "certs/trustStore");

        Producer producer = new Producer(HUB_HOSTNAME, HUB_PORT, EXCHANGE_NAME);
        producer.connect(tlsConf);
        producer.publish(routingKey, cisuMessage);
    }
}
